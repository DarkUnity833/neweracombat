package net.darkunity.neweracombat;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class CustomSlider extends AbstractSliderButton {
    private final Consumer<Double> onChanged;
    private final double minValue;
    private final double maxValue;
    private final String label;
    
    public CustomSlider(int x, int y, int width, int height, 
                       String label, double value, double min, double max,
                       Consumer<Double> onChanged) {
        super(x, y, width, height, Component.literal(label + String.format("%.1f", value)), 
              (value - min) / (max - min));
        this.label = label;
        this.minValue = min;
        this.maxValue = max;
        this.onChanged = onChanged;
    }
    
    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(label + String.format("%.1f", getValue())));
    }
    
    @Override
    protected void applyValue() {
        onChanged.accept(getValue());
    }
    
    public double getValue() {
        return minValue + (maxValue - minValue) * this.value;
    }
    
    public void setValue(double value) {
        this.value = (value - minValue) / (maxValue - minValue);
        updateMessage();
    }
}