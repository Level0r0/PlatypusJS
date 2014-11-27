/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.forms.components;

import com.eas.client.forms.api.components.HasValue;
import java.beans.PropertyChangeListener;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author Марат
 */
public class VRadioButtonMenuItem extends JRadioButtonMenuItem implements HasValue<Boolean> {

    private Boolean oldValue;

    public VRadioButtonMenuItem(String aText, boolean aSelected) {
        super(aText, aSelected);
        oldValue = aSelected;
        super.getModel().addChangeListener((ChangeEvent e) -> {
            checkValueChanged();
        });
    }

    private void checkValueChanged() {
        Boolean newValue = getValue();
        if (oldValue == null ? newValue != null : !oldValue.equals(newValue)) {
            Boolean wasOldValue = oldValue;
            oldValue = newValue;
            firePropertyChange(VALUE_PROP_NAME, wasOldValue, newValue);
        }
    }

    @Override
    public Boolean getValue() {
        return super.isSelected();
    }

    @Override
    public void setValue(Boolean aValue) {
        super.setSelected(aValue != null ? aValue : false);
    }

    @Override
    public void addValueChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(VALUE_PROP_NAME, listener);
    }

    private static final String VALUE_PROP_NAME = "value";

}
