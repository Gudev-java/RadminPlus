package com.radminplus.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpdateNotificationScreen extends Screen {
    private final Screen parent;

    public UpdateNotificationScreen(Screen parent) {
        super(Component.literal("Доступно обновление"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String githubText = UpdateChecker.updateGithubBtn != null ? UpdateChecker.updateGithubBtn : "Открыть на GitHub";
        String modrinthText = UpdateChecker.updateModrinthBtn != null ? UpdateChecker.updateModrinthBtn : "Открыть на Modrinth";
        String exitText = UpdateChecker.updateExitBtn != null ? UpdateChecker.updateExitBtn : "Выход из игры";
        String continueText = UpdateChecker.updateContinueBtn != null ? UpdateChecker.updateContinueBtn : "Продолжить";

        if (UpdateChecker.updateModrinthUrl != null && !UpdateChecker.updateModrinthUrl.isEmpty()) {
            // Three buttons side-by-side
            this.addRenderableWidget(Button.builder(Component.literal(githubText), btn -> {
                if (UpdateChecker.updateUrl != null) {
                    net.minecraft.util.Util.getPlatform().openUri(UpdateChecker.updateUrl);
                }
            }).bounds(this.width / 2 - 190, this.height - 40, 120, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(modrinthText), btn -> {
                net.minecraft.util.Util.getPlatform().openUri(UpdateChecker.updateModrinthUrl);
            }).bounds(this.width / 2 - 60, this.height - 40, 120, 20).build());

            if (UpdateChecker.mandatory) {
                this.addRenderableWidget(Button.builder(Component.literal(exitText), btn -> {
                    this.minecraft.stop();
                }).bounds(this.width / 2 + 70, this.height - 40, 120, 20).build());
            } else {
                this.addRenderableWidget(Button.builder(Component.literal(continueText), btn -> {
                    this.onClose();
                }).bounds(this.width / 2 + 70, this.height - 40, 120, 20).build());
            }
        } else {
            // Two buttons
            this.addRenderableWidget(Button.builder(Component.literal(githubText), btn -> {
                if (UpdateChecker.updateUrl != null) {
                    net.minecraft.util.Util.getPlatform().openUri(UpdateChecker.updateUrl);
                }
            }).bounds(this.width / 2 - 155, this.height - 40, 150, 20).build());

            if (UpdateChecker.mandatory) {
                this.addRenderableWidget(Button.builder(Component.literal(exitText), btn -> {
                    this.minecraft.stop();
                }).bounds(this.width / 2 + 5, this.height - 40, 150, 20).build());
            } else {
                this.addRenderableWidget(Button.builder(Component.literal(continueText), btn -> {
                    this.onClose();
                }).bounds(this.width / 2 + 5, this.height - 40, 150, 20).build());
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !UpdateChecker.mandatory;
    }

    @Override
    public void onClose() {
        UpdateChecker.userSkipped = true;
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        String titleText;
        if (UpdateChecker.updateTitle != null) {
            titleText = UpdateChecker.updateTitle;
        } else {
            titleText = UpdateChecker.mandatory ? "§c§lТРЕБУЕТСЯ КРИТИЧЕСКОЕ ОБНОВЛЕНИЕ RADMINPLUS!" : "§6§lДоступно новое обновление RadminPlus!";
        }

        // Replaced 16777215 with -1 (fully opaque white color in 1.21.11)
        guiGraphics.drawCenteredString(this.font, titleText, this.width / 2, 20, -1);
        guiGraphics.drawCenteredString(this.font, "§eВерсия: " + UpdateChecker.updateVersion, this.width / 2, 35, -1);
        guiGraphics.drawCenteredString(this.font, "§aСписок изменений:", this.width / 2, 60, -1);

        int startY = 80;
        if (UpdateChecker.updateChangelog != null) {
            for (String line : UpdateChecker.updateChangelog) {
                guiGraphics.drawCenteredString(this.font, "§f" + line, this.width / 2, startY, -1);
                startY += 12;
            }
        } else {
            guiGraphics.drawCenteredString(this.font, "§7Список изменений отсутствует.", this.width / 2, startY, -1);
        }
    }
}
