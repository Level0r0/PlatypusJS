/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.forms.components;

import com.eas.client.forms.IconCache;
import com.eas.client.forms.api.components.HasValue;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author Марат
 */
public class VCheckBox extends JCheckBox implements HasValue<Boolean>{
    
    protected static Icon nullIcon = IconCache.getIcon("16x16/nullCheck.gif");
    protected Icon ordinaryIcon;
    private Boolean oldValue;

    public VCheckBox(String aText, boolean aSelected) {
        super(aText, aSelected);
        ordinaryIcon = getIcon();
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
        return getIcon() == nullIcon ? null : super.isSelected();
    }

    @Override
    public void setValue(Boolean aValue) {
        if (aValue == null) {
            setIcon(nullIcon);
            super.setSelected(false);
        } else {
            setIcon(ordinaryIcon);
            super.setSelected(aValue);
        }
        checkValueChanged();
    }

    @Override
    public void addValueChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(VALUE_PROP_NAME, listener);
    }

    private static final String VALUE_PROP_NAME = "value";

}
