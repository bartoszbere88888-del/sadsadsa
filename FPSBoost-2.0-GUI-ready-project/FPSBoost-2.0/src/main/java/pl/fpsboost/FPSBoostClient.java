package pl.fpsboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
    private static int particleMode = 1; // 0=MINIMAL, 1=DECREASED, 2=ALL
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
                (handler, sender, mc) -> applyParticleSetting(mc));
    }

    static boolean isEnabled() { return enabled; }
    static int getParticleMode() { return particleMode; }

    static void toggleEnabled() {
        enabled = !enabled;
        saveConfig();
        applyParticleSetting(MinecraftClient.getInstance());
    }

    static void cycleParticles() {
        particleMode = (particleMode + 1) % 3;
        saveConfig();
        applyParticleSetting(MinecraftClient.getInstance());
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
        ParticleStatus status = !enabled ? ParticleStatus.ALL : switch (particleMode) {
            case 0 -> ParticleStatus.MINIMAL;
            case 1 -> ParticleStatus.DECREASED;
            default -> ParticleStatus.ALL;
        };
        client.options.getParticles().setValue(status);
    }

    private static void loadConfig() {
        try {
            if (configPath == null || !Files.exists(configPath)) return;
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            enabled = !json.contains("\"enabled\": false");
            if (json.contains("\"particles\": 0")) particleMode = 0;
            else if (json.contains("\"particles\": 1")) particleMode = 1;
            else particleMode = 2;
        } catch (IOException ignored) { }
    }

    private static void saveConfig() {
        try {
            if (configPath == null) return;
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath,
                    "{\n  \"enabled\": " + enabled + ",\n  \"particles\": " + particleMode + "\n}\n",
                    StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    private static final class FPSBoostScreen extends Screen {
        private final Screen parent;
        private int panelLeft, panelTop, panelWidth, panelHeight;

        FPSBoostScreen(Screen parent) {
            super(Text.literal("FPSBoost 2.0"));
            this.parent = parent;
        }

        @Override protected void init() {
            panelWidth = Math.min(360, width - 24);
            panelHeight = 244;
            panelLeft = (width - panelWidth) / 2;
            panelTop = Math.max(12, (height - panelHeight) / 2);
            int buttonWidth = Math.min(280, panelWidth - 36);
            int x = width / 2 - buttonWidth / 2;
            int y = panelTop + 80;
            addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
                toggleEnabled(); button.setMessage(enabledText());
            }).dimensions(x, y, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(particleText(), button -> {
                cycleParticles(); button.setMessage(particleText());
            }).dimensions(x, y + 28, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz ustawienia"), button -> {
                saveConfig(); button.setMessage(Text.literal("Zapisano ✓"));
            }).dimensions(x, y + 56, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Discord FPSBoost Studio"), button -> openDiscord())
                    .dimensions(x, y + 84, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Powrót"), button -> client.setScreen(parent))
                    .dimensions(x, y + 112, buttonWidth, 20).build());
        }

        private Text enabledText() {
            return Text.literal("Optymalizacja: " + (enabled ? "WŁĄCZONA" : "WYŁĄCZONA"));
        }
        private Text particleText() { return Text.literal("Cząsteczki: " + particleLabel()); }

        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            panelWidth = Math.min(360, width - 24);
            panelHeight = 244;
            panelLeft = (width - panelWidth) / 2;
            panelTop = Math.max(12, (height - panelHeight) / 2);
            context.fill(0, 0, width, height, 0x99070B14);
            context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xF0141B2B);
            context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 3, 0xFF55E6A5);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("FPSBOOST 2.0"), width / 2, panelTop + 17, 0xFF67F0B2);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("USTAWIENIA WYDAJNOŚCI"), width / 2, panelTop + 37, 0xFFEAF2FF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Klawisz menu: O  •  Ustawienia zapisują się w config"),
                    width / 2, panelTop + 61, 0xFF9BAAC3);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Status: " + (enabled ? "aktywny" : "wyłączony")),
                    width / 2, panelTop + panelHeight - 17, enabled ? 0xFF55E6A5 : 0xFFFFC36B);
        }

        @Override public void close() { client.setScreen(parent); }
    }
}
