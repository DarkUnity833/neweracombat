package net.darkunity.neweracombat;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class IntCustomSlider extends AbstractSliderButton {
    private final Consumer<Integer> onChanged;
    private final int minValue;
    private final int maxValue;
    private final String label;
    
    public IntCustomSlider(int x, int y, int width, int height, 
                          String label, int value, int min, int max,
                          Consumer<Integer> onChanged) {
        super(x, y, width, height, Component.literal(label + value), 
              (double)(value - min) / (max - min));
        this.label = label;
        this.minValue = min;
        this.maxValue = max;
        this.onChanged = onChanged;
    }
    
    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(label + getValue()));
    }
    
    @Override
    protected void applyValue() {
        onChanged.accept(getValue());
    }
    
    public int getValue() {
        return minValue + (int)((maxValue - minValue) * this.value);
    }
    
    public void setValue(int value) {
        this.value = (double)(value - minValue) / (maxValue - minValue);
        updateMessage();
    }
}