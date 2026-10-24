package pl.fpsboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleStatus;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FPSBoostClient implements ClientModInitializer {
    public static final String DISCORD_URL = "https://dc.gg/fpsbost.pl";
    private static KeyBinding openMenuKey;
    private static boolean enabled = true;
    private static int particleMode = 2; // 0=MINIMAL, 1=DECREASED, 2=ALL
    private static Path configPath;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        configPath = client.runDirectory.toPath().resolve("config/fpsboost.json");
        loadConfig();
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpsboost.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O,
                "key.categories.misc"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenuKey.wasPressed()) mc.setScreen(new FPSBoostScreen(mc.currentScreen));
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, mc) -> { if (enabled) applyParticleSetting(mc); });
    }

    static boolean isEnabled() { return enabled; }
    static int getParticleMode() { return particleMode; }

    static void toggleEnabled() {
        enabled = !enabled;
        saveConfig();
        if (enabled) applyParticleSetting(MinecraftClient.getInstance());
    }

    static void cycleParticles() {
        particleMode = (particleMode + 1) % 3;
        saveConfig();
        if (enabled) applyParticleSetting(MinecraftClient.getInstance());
    }

    static String particleLabel() {
        return switch (particleMode) {
            case 0 -> "Minimalne";
            case 1 -> "Zmniejszone";
            default -> "Wszystkie";
        };
    }

    static void openDiscord() {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(DISCORD_URL));
            else MinecraftClient.getInstance().keyboard.setClipboard(DISCORD_URL);
        } catch (Exception ignored) {
            MinecraftClient.getInstance().keyboard.setClipboard(DISCORD_URL);
        }
    }

    private static void applyParticleSetting(MinecraftClient client) {
        if (client == null || client.options == null) return;
        ParticleStatus status = switch (particleMode) {
            case 0 -> ParticleStatus.MINIMAL;
            case 1 -> ParticleStatus.DECREASED;
            default -> ParticleStatus.ALL;
        };
        client.options.getParticles().setValue(status);
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) return;
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            enabled = json.contains("\"enabled\": false") ? false : true;
            if (json.contains("\"particles\": 0")) particleMode = 0;
            else if (json.contains("\"particles\": 1")) particleMode = 1;
            else particleMode = 2;
        } catch (IOException ignored) { }
    }

    private static void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath,
                    "{\n  \"enabled\": " + enabled + ",\n  \"particles\": " + particleMode + "\n}\n",
                    StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    private static final class FPSBoostScreen extends Screen {
        private final Screen parent;
        FPSBoostScreen(Screen parent) { super(Text.literal("FPSBoost")); this.parent = parent; }

        @Override protected void init() {
            int center = width / 2;
            int y = height / 4;
            addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
                toggleEnabled(); button.setMessage(enabledText());
            }).dimensions(center - 100, y, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(particleText(), button -> {
                cycleParticles(); button.setMessage(particleText());
            }).dimensions(center - 100, y + 28, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Dołącz na Discord"), button -> openDiscord())
                    .dimensions(center - 100, y + 56, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Powrót"), button -> client.setScreen(parent))
                    .dimensions(center - 100, y + 84, 200, 20).build());
        }

        private Text enabledText() { return Text.literal("FPSBoost: " + (enabled ? "WŁĄCZONY" : "WYŁĄCZONY")); }
        private Text particleText() { return Text.literal("Cząsteczki: " + particleLabel()); }

        @Override public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 4 - 24, 0x55FF55);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override public void close() { client.setScreen(parent); }
    }
}
