package net.darkunity.neweracombat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CombatConfigScreen extends Screen {
    private final Screen lastScreen;
    
    private Button cooldownsToggle;
    private Button barsToggle;
    private Button alternateToggle;
    private Button textToggle;
    private Button doneButton;
    private Button defaultsButton;
    
    private CustomSlider mainHandSlider;
    private CustomSlider offHandSlider;
    private CustomSlider shieldSlider;
    private IntCustomSlider offsetSlider;
    
    private EditBox widthBox;
    private EditBox heightBox;

    public CombatConfigScreen(Screen parent) {
        super(Component.translatable("config.neweracombat.title"));
        this.lastScreen = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int y = 40;
        int spacing = 25;
        
        this.addRenderableWidget(new SimpleLabel(centerX, 20, buttonWidth, 20, 
            Component.literal("§6New Era Combat - Настройки"), true));
        
        cooldownsToggle = Button.builder(
            getToggleText("Кулдауны", CombatConfig.enableCooldowns()),
            button -> {
                CombatConfig.CONFIG.enableCooldowns.set(!CombatConfig.enableCooldowns());
                button.setMessage(getToggleText("Кулдауны", CombatConfig.enableCooldowns()));
                updateSliders();
                CombatConfig.save();
            })
            .pos(centerX - buttonWidth / 2, y)
            .size(buttonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(cooldownsToggle);
        y += spacing;
        
        barsToggle = Button.builder(
            getToggleText("Полоски кулдауна", CombatConfig.showCooldownBars()),
            button -> {
                CombatConfig.CONFIG.showCooldownBars.set(!CombatConfig.showCooldownBars());
                button.setMessage(getToggleText("Полоски кулдауна", CombatConfig.showCooldownBars()));
                CombatConfig.save();
            })
            .pos(centerX - buttonWidth / 2, y)
            .size(buttonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(barsToggle);
        y += spacing;
        
        alternateToggle = Button.builder(
            getToggleText("Чередовать руки", CombatConfig.alternateHandAttacks()),
            button -> {
                CombatConfig.CONFIG.alternateHandAttacks.set(!CombatConfig.alternateHandAttacks());
                button.setMessage(getToggleText("Чередовать руки", CombatConfig.alternateHandAttacks()));
                CombatConfig.save();
            })
            .pos(centerX - buttonWidth / 2, y)
            .size(buttonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(alternateToggle);
        y += spacing;
        
        textToggle = Button.builder(
            getToggleText("Текст подсказки", CombatConfig.showCooldownText()),
            button -> {
                CombatConfig.CONFIG.showCooldownText.set(!CombatConfig.showCooldownText());
                button.setMessage(getToggleText("Текст подсказки", CombatConfig.showCooldownText()));
                CombatConfig.save();
            })
            .pos(centerX - buttonWidth / 2, y)
            .size(buttonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(textToggle);
        y += spacing + 10;
        
        this.addRenderableWidget(new SimpleLabel(centerX, y, buttonWidth, 20, 
            Component.literal("§eНастройки кулдаунов (секунды)"), true));
        y += 25;
        
        mainHandSlider = new CustomSlider(
            centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            "Правая рука: ", 
            CombatConfig.getMainHandCooldown(), 
            0.1, 5.0, 
            value -> {
                CombatConfig.CONFIG.mainHandCooldown.set(value);
                CombatConfig.save();
            }
        );
        this.addRenderableWidget(mainHandSlider);
        y += spacing;
        
        offHandSlider = new CustomSlider(
            centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            "Левая рука: ", 
            CombatConfig.getOffHandCooldown(), 
            0.1, 5.0, 
            value -> {
                CombatConfig.CONFIG.offHandCooldown.set(value);
                CombatConfig.save();
            }
        );
        this.addRenderableWidget(offHandSlider);
        y += spacing;
        
        shieldSlider = new CustomSlider(
            centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            "Удар щитом: ", 
            CombatConfig.getShieldBashCooldown(), 
            0.1, 5.0, 
            value -> {
                CombatConfig.CONFIG.shieldBashCooldown.set(value);
                CombatConfig.save();
            }
        );
        this.addRenderableWidget(shieldSlider);
        y += spacing + 10;
        
        this.addRenderableWidget(new SimpleLabel(centerX, y, buttonWidth, 20, 
            Component.literal("§eВизуальные настройки"), true));
        y += 25;
        
        offsetSlider = new IntCustomSlider(
            centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            "Расстояние от прицела: ", 
            CombatConfig.getBarOffset(), 
            10, 50, 
            value -> {
                CombatConfig.CONFIG.barOffset.set(value);
                CombatConfig.save();
            }
        );
        this.addRenderableWidget(offsetSlider);
        y += spacing;
        
        int fieldWidth = 95;
        
        widthBox = new EditBox(this.font, centerX - buttonWidth / 2, y, fieldWidth, buttonHeight,
            Component.literal("Ширина полоски"));
        widthBox.setValue(String.valueOf(CombatConfig.getBarWidth()));
        widthBox.setMaxLength(3);
        widthBox.setFilter(s -> s.matches("\\d*"));
        widthBox.setResponder(s -> {
            if (!s.isEmpty()) {
                int value = Integer.parseInt(s);
                value = Math.max(2, Math.min(10, value));
                CombatConfig.CONFIG.barWidth.set(value);
                widthBox.setValue(String.valueOf(value));
                CombatConfig.save();
            }
        });
        this.addRenderableWidget(widthBox);
        
        heightBox = new EditBox(this.font, centerX - buttonWidth / 2 + fieldWidth + 10, y, fieldWidth, buttonHeight,
            Component.literal("Высота полоски"));
        heightBox.setValue(String.valueOf(CombatConfig.getBarHeight()));
        heightBox.setMaxLength(3);
        heightBox.setFilter(s -> s.matches("\\d*"));
        heightBox.setResponder(s -> {
            if (!s.isEmpty()) {
                int value = Integer.parseInt(s);
                value = Math.max(10, Math.min(40, value));
                CombatConfig.CONFIG.barHeight.set(value);
                heightBox.setValue(String.valueOf(value));
                CombatConfig.save();
            }
        });
        this.addRenderableWidget(heightBox);
        y += spacing + 10;
        
        int bottomButtonWidth = 95;
        
        defaultsButton = Button.builder(
            Component.literal("По умолчанию"),
            button -> resetToDefaults())
            .pos(centerX - bottomButtonWidth - 5, y)
            .size(bottomButtonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(defaultsButton);
        
        doneButton = Button.builder(
            CommonComponents.GUI_DONE,
            button -> onClose())
            .pos(centerX + 5, y)
            .size(bottomButtonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(doneButton);
        
        updateSliders();
    }
    
    private Component getToggleText(String text, boolean enabled) {
        return Component.literal(text + ": " + (enabled ? "§aВКЛ" : "§cВЫКЛ"));
    }
    
    private void updateSliders() {
        boolean enabled = CombatConfig.enableCooldowns();
        mainHandSlider.active = enabled;
        offHandSlider.active = enabled;
        shieldSlider.active = enabled;
    }
    
    private void resetToDefaults() {
        CombatConfig.CONFIG.enableCooldowns.set(true);
        CombatConfig.CONFIG.showCooldownBars.set(true);
        CombatConfig.CONFIG.alternateHandAttacks.set(true);
        CombatConfig.CONFIG.showCooldownText.set(true);
        
        CombatConfig.CONFIG.mainHandCooldown.set(0.6);
        CombatConfig.CONFIG.offHandCooldown.set(0.8);
        CombatConfig.CONFIG.shieldBashCooldown.set(1.0);
        
        CombatConfig.CONFIG.barOffset.set(18);
        CombatConfig.CONFIG.barWidth.set(4);
        CombatConfig.CONFIG.barHeight.set(20);
        
        cooldownsToggle.setMessage(getToggleText("Кулдауны", true));
        barsToggle.setMessage(getToggleText("Полоски кулдауна", true));
        alternateToggle.setMessage(getToggleText("Чередовать руки", true));
        textToggle.setMessage(getToggleText("Текст подсказки", true));
        
        mainHandSlider.setValue(0.6);
        offHandSlider.setValue(0.8);
        shieldSlider.setValue(1.0);
        offsetSlider.setValue(18);
        
        widthBox.setValue("4");
        heightBox.setValue("20");
        
        updateSliders();
        CombatConfig.save();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        
        int panelX = this.width / 2 - 125;
        int panelY = 30;
        int panelWidth = 250;
        int panelHeight = this.height - 60;
        
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 
            0x66000000);
        
        graphics.hLine(panelX, panelX + panelWidth, panelY, 0xFF555555);
        graphics.hLine(panelX, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        graphics.vLine(panelX, panelY, panelY + panelHeight, 0xFF555555);
        graphics.vLine(panelX + panelWidth, panelY, panelY + panelHeight, 0xFF555555);
        
        super.render(graphics, mouseX, mouseY, partialTick);
        
        if (cooldownsToggle.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Включить или отключить систему кулдаунов для оружия"), 
                mouseX, mouseY);
        } else if (barsToggle.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Показывать полоски кулдаунов рядом с прицелом"), 
                mouseX, mouseY);
        } else if (alternateToggle.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Чередовать атаки левой и правой рукой при оружии в обеих руках"), 
                mouseX, mouseY);
        } else if (textToggle.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Показывать текст с процентами или секундами кулдауна"), 
                mouseX, mouseY);
        } else if (mainHandSlider.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Время кулдауна для правой руки (секунды)"), 
                mouseX, mouseY);
        } else if (offHandSlider.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Время кулдауна для левой руки (секунды)"), 
                mouseX, mouseY);
        } else if (shieldSlider.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, 
                Component.literal("Время кулдауна для удара щитом (секунды)"), 
                mouseX, mouseY);
        }
    }
    
    @Override
    public void onClose() {
        CombatConfig.save();
        super.onClose();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private static class SimpleLabel extends AbstractWidget {
        private final Component text;
        private final boolean centered;
        
        public SimpleLabel(int x, int y, int width, int height, Component text, boolean centered) {
            super(x, y, width, height, text);
            this.text = text;
            this.centered = centered;
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (centered) {
                graphics.drawCenteredString(Minecraft.getInstance().font, this.text, 
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF);
            } else {
                graphics.drawString(Minecraft.getInstance().font, this.text, 
                    this.getX(), this.getY() + (this.height - 8) / 2, 0xFFFFFF);
            }
        }
        
        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        }
    }
}