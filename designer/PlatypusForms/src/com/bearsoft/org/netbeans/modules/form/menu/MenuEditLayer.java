/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package com.bearsoft.org.netbeans.modules.form.menu;

import com.bearsoft.org.netbeans.modules.form.*;
import com.bearsoft.org.netbeans.modules.form.actions.PropertyAction;
import com.bearsoft.org.netbeans.modules.form.editors.IconEditor.NbImageIcon;
import com.bearsoft.org.netbeans.modules.form.palette.PaletteItem;
import com.bearsoft.org.netbeans.modules.form.palette.PaletteUtils;
import com.eas.client.forms.menu.MenuItem;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.PopupMenuUI;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOp;
import org.openide.util.NbBundle;

/**
 *
 * @author joshua.marinacci@sun.com
 */
public class MenuEditLayer extends JPanel {
    /* === constants for the look of the designer === */

    public static final Border DRAG_MENU_BORDER = BorderFactory.createLineBorder(Color.BLACK, 1);
    public static final Border DRAG_SEPARATOR_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
    public static final Color SELECTED_MENU_BACKGROUND = new Color(0xA5A6A9);
    public static final Color EMPTY_ICON_COLOR = new Color(0xDDDDDD);
    public static final int EMPTY_ICON_BORDER_WIDTH = 2;
    /* === private constants === */
    private static final boolean USE_NEW_ITEM_COLOR_SWITCHING = false;
    /* === public and package level fields. these should probably become getters and setters  ===*/
    VisualDesignerPopupFactory hackedPopupFactory = null;
    PlatypusFormLayoutView formDesigner;
    JLayeredPane layers;
    JComponent glassLayer;
    DropTargetLayer dropTargetLayer;
    boolean showMenubarWarning = false;
    /* === private fields === */
    private Map<JMenu, PopupMenuUI> menuPopupUIMap;

    public enum SelectedPortion {

        Icon, Text, Accelerator, All, None
    };
    private SelectedPortion selectedPortion = SelectedPortion.None;
    private KeyboardMenuNavigator keyboardMenuNavigator;
    private Map<RADVisualContainer<?>, FormModelListener> formModelListeners;
    private DragOperation dragop;
    private FormModelListener menuBarFormListener;
    private PropertyChangeListener selectionListener;
    private boolean isAlive = true;
    private static final boolean USE_JSEPARATOR_FIX = true;

    /**
     * Creates a new instance of MenuEditLayer
     */
    public MenuEditLayer(final PlatypusFormLayoutView aFormDesigner) {
        formDesigner = aFormDesigner;
        menuPopupUIMap = new HashMap<>();
        formModelListeners = new HashMap<>();

        layers = new JLayeredPane();
        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);
        dragop = new DragOperation(this);
        glassLayer = new JComponent() {
            @Override
            public void paintComponent(Graphics g) {
            }
        };
        layers.add(glassLayer, new Integer(500)); // put the glass layer over the drag layer
        glassLayer.setSize(400, 400); //josh: do i need this line? probably can delete it.
        dropTargetLayer = new DropTargetLayer(this);
        layers.add(dropTargetLayer, new Integer(JLayeredPane.DRAG_LAYER - 5)); // put the drop target layer just above the drag layer
        // make the extra layers resize to the main component
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                glassLayer.setSize(MenuEditLayer.this.getSize());
                dropTargetLayer.setSize(MenuEditLayer.this.getSize());
            }
        });
        MouseInputAdapter mia = new GlassLayerMouseListener();
        glassLayer.addMouseListener(mia);
        glassLayer.addMouseMotionListener(mia);
        configureSelectionListener();
    }

    DragOperation getDragOperation() {
        return dragop;
    }

    public static boolean isMenuRelatedRADComponent(RADComponent<?> comp) {
        return comp != null && isMenuRelatedComponentClass(comp.getBeanClass());
    }

    public static boolean isNonMenuJSeparator(RADComponent<?> comp) {
        if (comp == null) {
            return false;
        }
        if (JSeparator.class.isAssignableFrom(comp.getBeanClass())) {
            RADComponent<?> parent = comp.getParentComponent();
            if (parent != null && JMenu.class.isAssignableFrom(parent.getBeanClass())) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean isMenuBarContainer(RADComponent<?> comp) {
        if (comp == null) {
            return false;
        }
        Class<?> clazz = comp.getBeanClass();
        if (clazz == null) {
            return false;
        }
        if (JMenuBar.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    public static boolean isMenuRelatedContainer(RADComponent<?> comp) {
        if (comp == null) {
            return false;
        }
        Class<?> clas = comp.getBeanClass();
        if (clas == null) {
            return false;
        }
        if (JMenu.class.isAssignableFrom(clas)) {
            return true;
        }
        if (JPopupMenu.class.isAssignableFrom(clas)) {
            return true;
        }
        return false;
    }

    public static boolean isMenuRelatedComponentClass(Class<?> clas) {
        if (clas == null) {
            return false;
        }
        if (JMenuItem.class.isAssignableFrom(clas)) {
            return true;
        }
        if (JMenu.class.isAssignableFrom(clas)) {
            return true;
        }
        if (JSeparator.class.isAssignableFrom(clas)) {
            return true;
        }
        if (JMenuBar.class.isAssignableFrom(clas)) {
            return true;
        }
        return false;
    }

    public boolean isPossibleNewMenuComponent(PaletteItem item) {
        if (item == null) {
            return false;
        }
        if (item.getComponentClass() == null) {
            return false;
        }
        if (JMenuItem.class.isAssignableFrom(item.getComponentClass())) {
            return true;
        }
        return false;
    }

    public void startNewMenuComponentPickAndPlop(PaletteItem item, Point pt) {
        this.setVisible(true);
        this.requestFocus();
        dragop = new DragOperation(this);
        dragop.start(item, pt);
    }

    public void startNewMenuComponentDragAndDrop(PaletteItem item) {
        this.setVisible(true);
        this.requestFocus();
        configureGlassLayer();
        configureFormListeners();
    }

    // the public method for non-menu parts of the form editor to
    // start menu editing
    public void openAndShowMenu(RADComponent<?> radComp, Component comp) {
        //p("making sure the menu is open: " + radComp +  " " + radComp.getName());
        if (hackedPopupFactory == null) {
            hackedPopupFactory = new VisualDesignerPopupFactory(this);
        }
        openMenu(radComp, comp);
        glassLayer.requestFocusInWindow();
    }

    void openMenu(RADComponent<?> radComp, Component comp) {
        getPopupFactory();
        configureGlassLayer();
        registerKeyListeners();
        configureFormListeners();
        configureSelectionListener();
        //reset the layers
        JMenu menu = (JMenu) comp;
        configureMenu(null, menu);
        showMenuPopup(menu);
        if (radComp instanceof RADVisualContainer<?>) {
            keyboardMenuNavigator.setCurrentMenuRAD((RADVisualContainer<?>) radComp);
        }
    }

    public void hideMenuLayer() {
        // tear down each menu and menu item
        unconfigureFormListeners();
        unconfigureSelectionListener();
        for (JMenu m : menuPopupUIMap.keySet()) {
            unconfigureMenu(m);
        }
        menuPopupUIMap.clear();
        if (hackedPopupFactory != null) {
            hackedPopupFactory.containerMap.clear();
            hackedPopupFactory = null;
        }
        if (dragop.isStarted()) {
            dragop.fastEnd();
        }
        // close all popup frames
        this.setVisible(false);
        if (keyboardMenuNavigator != null) {
            glassLayer.removeKeyListener(keyboardMenuNavigator);
            keyboardMenuNavigator.unconfigure();
            keyboardMenuNavigator = null;
        }
        backgroundMap.clear();
        //hackedPopupFactory.containerMap.clear();
        if (formDesigner.getHandleLayer() != null) {
            formDesigner.getHandleLayer().requestFocusInWindow();
        }
    }

    //josh: all this key listener stuff should go into a separate class
    private synchronized void registerKeyListeners() {
        if (keyboardMenuNavigator == null) {
            keyboardMenuNavigator = new KeyboardMenuNavigator(this);
            glassLayer.addKeyListener(keyboardMenuNavigator);
            glassLayer.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        dragop.fastEnd();
                    }
                }
            });
        }
    }

    private VisualDesignerPopupFactory getPopupFactory() {
        if (hackedPopupFactory == null) {
            hackedPopupFactory = new VisualDesignerPopupFactory(this);
        }
        return hackedPopupFactory;
    }

    private void configureGlassLayer() {
        try {
            glassLayer.setDropTarget(new DropTarget());
            glassLayer.getDropTarget().addDropTargetListener(new GlassLayerDropTargetListener());
        } catch (TooManyListenersException ex) {
            ErrorManager.getDefault().notify(ex);
        }
    }
    PropertyChangeListener paletteListener = null;

    private void configureFormListeners() {

        if (menuBarFormListener == null) {
            menuBarFormListener = new FormModelListener() {
                @Override
                public void formChanged(FormModelEvent[] events) {
                    if (events != null) {
                        for (FormModelEvent evt : events) {
                            // if this is a menubar delete event
                            if (evt.getChangeType() == FormModelEvent.COMPONENT_REMOVED) {
                                if (evt.getComponent() != null
                                        && JMenuBar.class.isAssignableFrom(evt.getComponent().getBeanClass())) {
                                    hideMenuLayer();
                                }
                            }
                            if (evt.getChangeType() == FormModelEvent.FORM_TO_BE_CLOSED) {
                                hideMenuLayer();
                                isAlive = false;
                            }
                            if (evt.getChangeType() == FormModelEvent.COMPONENT_ADDED) {
                                if (evt.getCreatedDeleted()) {
                                    if (USE_NEW_ITEM_COLOR_SWITCHING) {
                                        configureNewComponent(evt.getComponent());
                                    }
                                }
                            }

                        }
                    }
                }
            };
            formDesigner.getFormModel().addFormModelListener(menuBarFormListener);
        }
        if (paletteListener == null) {
            paletteListener = (PropertyChangeEvent evt) -> {
                if (PaletteUtils.getSelectedItem() == null
                        || !isMenuRelatedComponentClass(PaletteUtils.getSelectedItem().getComponentClass())) {
                    if (dragop != null && dragop.isStarted()) {
                        dragop.fastEnd();
                    }
                }
            };
            paletteContext = formDesigner.getFormEditor().getFormDataObject().getPrimaryFile();
            PaletteUtils.addPaletteListener(paletteListener, paletteContext);
        }
    }
    FileObject paletteContext = null;

    private void unconfigureFormListeners() {
        if (menuBarFormListener != null) {
            if (formDesigner != null && formDesigner.getFormModel() != null) {
                formDesigner.getFormModel().removeFormModelListener(menuBarFormListener);
            }
        }
        if (paletteListener != null) {
            PaletteUtils.removePaletteListener(paletteListener, paletteContext);
            paletteContext = null;
            paletteListener = null;
        }
        menuBarFormListener = null;
    }

    private void configureSelectionListener() {
        if (selectionListener == null) {
            selectionListener = (PropertyChangeEvent evt) -> {
                if (isAlive) {
                    Node[] newNodes = (Node[]) evt.getNewValue();
                    List<RADVisualComponent<?>> selectedNodes = new ArrayList<>();
                    for (Node n : newNodes) {
                        if (n instanceof RADComponentNode) {
                            RADComponentNode radn = (RADComponentNode) n;
                            if (radn.getRADComponent() instanceof RADVisualComponent<?>) {
                                selectedNodes.add((RADVisualComponent<?>) radn.getRADComponent());
                            }
                        }
                    }
                    setSelectedRADComponents(selectedNodes);
                }
            };
            formDesigner.addPropertyChangeListener("activatedNodes", selectionListener); // NOI18N
        }
    }

    private void unconfigureSelectionListener() {
        if (selectionListener != null) {
            formDesigner.removePropertyChangeListener("activatedNodes", selectionListener); // NOI18N
            selectionListener = null;
        }
    }

    void showMenuPopup(final JMenu menu) {
        getPopupFactory();
        // if already created then just make it visible
        if (hackedPopupFactory.containerMap.containsKey(menu)) {
            JPanel view = hackedPopupFactory.containerMap.get(menu);
            view.setVisible(true);
        } else {
            if (!isConfigured(menu)) {
                configureMenu(null, menu);
            }
            final JPopupMenu popup = menu.getPopupMenu();

            if (!(popup.getUI() instanceof VisualDesignerPopupMenuUI)) {
                popup.setUI(new VisualDesignerPopupMenuUI(this, popup.getUI()));
            }
            if (menu.isShowing()) {
                //force popup view creation
                hackedPopupFactory.getPopup(menu, null, 0, 0);

                // do later so that the component will definitely be on screen by then
                java.awt.EventQueue.invokeLater(() -> {
                    try {
                        popup.show(menu, 0, menu.getHeight());
                    } catch (Exception ex) {
                        ErrorManager.getDefault().notify(ex);
                        //ignore anyexceptions caused by showing the popups
                    }
                });
            }
        }
        this.validate();
    }

    public boolean isMenuLayerComponent(RADComponent<?> radComp) {
        if (radComp == null) {
            return false;
        }
        if (radComp.getBeanClass().equals(JMenuItem.class)) {
            return true;
        }
        if (radComp.getBeanClass().equals(JMenu.class)) {
            return true;
        }
        return false;
    }

    void configureMenu(final JComponent parent, final JMenu menu) {
        // make sure it will draw it's border so we can have rollovers and selection
        menu.setBorderPainted(true);
        //install the wrapper icon if not a toplevel JMenu
        if (!isTopLevelMenu(menu)) {
            if (!(menu.getIcon() instanceof WrapperIcon)) {
                menu.setIcon(new WrapperIcon(menu.getIcon()));
            }
        }

        // configure the maps and popups
        JPopupMenu popup = menu.getPopupMenu();
        menuPopupUIMap.put(menu, popup.getUI());
        popup.setUI(new VisualDesignerPopupMenuUI(this, popup.getUI()));

        // get all of the components in this menu
        Component[] subComps = menu.getMenuComponents();
        // if this isn't the first time this menu has been opened then the sub components
        // will have been moved to the popupPanel already, so we will find them there instead.
        JPanel popupPanel = getPopupFactory().containerMap.get(menu);
        if (popupPanel != null) {
            subComps = popupPanel.getComponents();
        }

        RADVisualContainer<?> menuRAD = (RADVisualContainer<?>) formDesigner.getRadComponent(menu);
        registerForm(menuRAD, menu);

        // recurse for sub-menus
        for (Component c : subComps) {
            if (c instanceof JMenu) {
                configureMenu(menu, (JMenu) c);
                RADComponent<?> rad = formDesigner.getRadComponent(c);
                registerForm((RADVisualContainer<?>) rad, (JMenu) c);
            } else {
                configureMenuItem(menu, (JComponent) c);
            }
        }
    }

    private void unconfigureMenu(final JMenu menu) {
        if (hackedPopupFactory == null) {
            return; // Issue 145981
        }
        // restore the UI
        menu.getPopupMenu().setUI(menuPopupUIMap.get(menu));

        // restore all children
        JPanel popup = hackedPopupFactory.containerMap.get(menu);
        if (popup != null) {
            for (Component c : popup.getComponents()) {
                if (c instanceof JMenu) {
                    unconfigureMenu((JMenu) c);
                } else {
                    unconfigureMenuItem((JComponent) c);
                }
            }

            //hide the popup(s) if it's still visible
            if (menu.getPopupMenu() != null) {
                menu.getPopupMenu().setVisible(false);
            }
            popup.setVisible(false);
            //layers.remove(popup);
        }
        VisualDesignerJPanelPopup pop = hackedPopupFactory.getPopup(menu);
        if (pop != null) {
            pop.hide();
        }
        if (popup != null) {
            popup.setVisible(false);
        }
        menu.setPopupMenuVisible(false);
        hackedPopupFactory.containerMap.remove(menu);
    }

    private boolean isConfigured(Component c) {
        return c instanceof JMenu && menuPopupUIMap.containsKey((JMenu) c);
    }

    void configureMenuItem(final JMenu parent, final Component c) {
        if (c instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) c;
            if (!(item.getIcon() instanceof WrapperIcon)) {
                item.setIcon(new WrapperIcon(item.getIcon()));
            }
            installAcceleratorPreview(item);
            item.setBorderPainted(true);
        }
    }
    static final int ACCEL_PREVIEW_WIDTH = 80;
    private static final Border accel_border = new Border() {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(Color.WHITE);
            int offset = 5;
            if (DropTargetLayer.isAqua()) {
                offset = 2;
            }
            int ioffset = 0;
            if (DropTargetLayer.isVista()) {
                ioffset = -2;
            }
            g.fillRect(width - ACCEL_PREVIEW_WIDTH + offset, 1, ACCEL_PREVIEW_WIDTH - 0 + ioffset, height + ioffset);
            g.setColor(EMPTY_ICON_COLOR);
            g.drawRect(width - ACCEL_PREVIEW_WIDTH + offset, 1, ACCEL_PREVIEW_WIDTH - 1 + ioffset, height + ioffset);
            g.drawRect(width - ACCEL_PREVIEW_WIDTH + offset + 1, 2, ACCEL_PREVIEW_WIDTH - 3 + ioffset, height - 2 + ioffset);
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10)); // NOI18N
            String shortcut = NbBundle.getMessage(MenuEditLayer.class, "MENU_Shortcut"); // NOI18N
            g.drawString(shortcut, width - ACCEL_PREVIEW_WIDTH + 15, height - 3 + ioffset);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 0, ACCEL_PREVIEW_WIDTH);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    };

    //installs a special border to represent the accelerator preview
    //if the menu item already has an accelerator, then it will
    //remove the preview if necessary.
    private static void installAcceleratorPreview(JMenuItem item) {
        if (item instanceof JMenu) {
            return;
        }
        //detect accelerator key
        boolean already_has_accel = false;
        if (item.getAccelerator() != null) {
            already_has_accel = true;
        }
        if (item.getAction() != null && item.getAction().getValue(Action.ACCELERATOR_KEY) != null) {
            already_has_accel = true;
        }



        boolean already_has_accel_border = false;
        if (item.getBorder() == accel_border) {
            already_has_accel_border = true;
            //uninstall if needed
            if (already_has_accel) {
                item.setBorder(null);
                return;
            }
        }

        if (item.getBorder() instanceof CompoundBorder) {
            CompoundBorder comp = (CompoundBorder) item.getBorder();
            if (comp.getInsideBorder() == accel_border) {
                already_has_accel_border = true;
                //uninstall if needed
                if (already_has_accel) {
                    item.setBorder(comp.getOutsideBorder());
                    return;
                }
            }
        }

        if (already_has_accel_border) {
            return;
        }
        if (already_has_accel) {
            return;
        }


        if (item.getBorder() == null) {
            item.setBorder(accel_border);
            return;
        }

        item.setBorder(BorderFactory.createCompoundBorder(
                item.getBorder(), accel_border));
    }

    void unconfigureMenuItem(JComponent c) {
    }

    //override JComponent.isOpaque to always return false
    @Override
    public boolean isOpaque() {
        return false;
    }

    // returns true if parent really is an ancestor of target
    boolean isAncestor(JComponent target, JComponent parent) {
        if (!(parent instanceof JMenu)) {
            return false;
        }
        RADComponent<?> targetRad = formDesigner.getRadComponent(target);
        RADComponent<?> parentRad = targetRad.getParentComponent();
        if (parentRad == null) {
            return false;
        }
        Component possibleParent = formDesigner.getComponent(parentRad);
        RADComponent<?> realParentRad = formDesigner.getRadComponent(parent);
        if (parentRad == realParentRad) {
            return true;
        }
        if (parent == possibleParent) {
            return true;
        } else {
            // recursively check up the chain to see if this is a further ancestor
            if (possibleParent instanceof JMenu) {
                return isAncestor((JMenu) possibleParent, parent);
            }
        }
        return false;
    }

    boolean hasSelectedDescendants(JMenu menu) {
        RADComponent<?> comp = formDesigner.getRadComponent(menu);
        if (comp instanceof RADVisualContainer<?>) {
            return hasSelectedDescendants((RADVisualContainer<?>) comp);
        }
        return false;
    }

    boolean hasSelectedDescendants(RADVisualContainer<?> comp) {
        if (this.selectedComponents.contains(comp)) {
            return true;
        }
        for (RADVisualComponent<?> c : comp.getSubBeans()) {
            if (this.selectedComponents.contains(c)) {
                return true;
            }
            if (c instanceof RADVisualContainer<?>) {
                boolean sel = hasSelectedDescendants((RADVisualContainer<?>) c);
                if (sel) {
                    return true;
                }
            }
        }
        return false;
    }

    JComponent getMenuParent(JComponent menu) {
        RADComponent<?> targetRad = formDesigner.getRadComponent(menu);
        RADComponent<?> parentRad = targetRad.getParentComponent();
        if (parentRad != null) {
            Component possibleParent = formDesigner.getComponent(parentRad);
            if (possibleParent instanceof JComponent) {
                return (JComponent) possibleParent;
            }
        }
        return null;
    }

    List<RADVisualComponent<?>> getSelectedRADComponents() {
        return Collections.unmodifiableList(selectedComponents);
    }

    RADVisualComponent<?> getSingleSelectedComponent() {
        if (selectedComponents.isEmpty()) {
            return null;
        }
        if (selectedComponents.size() > 1) {
            setSelectedRADComponent(selectedComponents.get(0));
        }
        return selectedComponents.get(0);
    }
    private List<RADVisualComponent<?>> selectedComponents = new ArrayList<>();

    boolean isComponentSelected() {
        return !selectedComponents.isEmpty();
    }

    void setSelectedRADComponent(RADVisualComponent<?> comp) {
        List<RADVisualComponent<?>> comps = new ArrayList<>();
        comps.add(comp);
        setSelectedRADComponents(comps);
        formDesigner.setSelectedComponent(comp);
    }

    void addSelectedRADComponent(RADVisualComponent<?> comp) {
        List<RADVisualComponent<?>> comps = new ArrayList<>();
        comps.addAll(selectedComponents);
        comps.add(comp);
        setSelectedRADComponents(comps);
        formDesigner.addComponentToSelection(comp);
    }

    void setSelectedRADComponents(List<RADVisualComponent<?>> comps) {
        try {
            //clear old bgs first
            for (RADComponent<?> rad : selectedComponents) {
                if (isMenuRelatedRADComponent(rad) && !isMenuBarContainer(rad) && !isNonMenuJSeparator(rad)) { // don't mess w/ the menubar's background
                    Component c = formDesigner.getComponent(rad);
                    if (c != null) { // could be null if comp was just deleted
                        c.setBackground(getNormalBackground(rad, c));
                    }
                }
            }
            selectedComponents.clear();
            selectedComponents.addAll(comps);
            //check for non-menu comps
            for (RADComponent<?> c : selectedComponents) {
                if (!isMenuRelatedRADComponent(c) || isMenuBarContainer(c) || isNonMenuJSeparator(c)) {
                    setVisible(false);
                    return;
                }
            }
            registerKeyListeners();
            for (RADComponent<?> rad : selectedComponents) {
                Component c = formDesigner.getComponent(rad);
                if (c != null) {
                    if (!isMenuBarContainer(rad)) { // don't mess w/ the menubar's background
                        c.setBackground(getSelectedBackground(c));
                    }
                    makeSureShowingOnScreen(rad, c);
                    if (c instanceof JMenu) {
                        showMenuPopup((JMenu) c);
                    }
                }
            }
            repaint();
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
        }
    }

    private String getComponentDefaultsPrefix(Component c) {
        if (c instanceof JMenuBar) {
            return "MenuBar"; // NOI18N
        }
        if (c instanceof JMenu) {
            return "Menu"; // NOI18N
        }
        if (c instanceof JCheckBoxMenuItem) {
            return "CheckBoxMenuItem"; // NOI18N
        }
        if (c instanceof JRadioButtonMenuItem) {
            return "RadioButtonMenuItem"; // NOI18N
        }
        return "MenuItem"; // NOI18N
    }

    private Color getNormalBackground(RADComponent<?> radComp, Component c) {
        RADProperty<Color> prop = radComp.<RADProperty<Color>>getProperty("background"); // NOI18N
        Color color = null;
        if (prop != null) {
            try {
                color = prop.getValue();
            } catch (Exception ex) {
            }
        }
        if (color == null) {
            // fallback - for example subclass of menu component
            // that hides background property
            color = backgroundMap.get(c);
        }
        return color;
    }
    private Map<Component, Color> backgroundMap = new HashMap<>();

    private Color getSelectedBackground(Component c) {
        //don't put into the map twice
        if (!backgroundMap.containsKey(c)) {
            backgroundMap.put(c, c.getBackground());
        }
        return SELECTED_MENU_BACKGROUND;
    }

    private Color getNormalForeground(Component c) {
        String prefix = getComponentDefaultsPrefix(c);
        Color color = UIManager.getDefaults().getColor(prefix + ".foreground"); // NOI18N
        if (color == null) {
            color = Color.BLACK;
        }
        return color;
    }

    private void makeSureShowingOnScreen(RADComponent<?> rad, Component comp) {
        if (!this.isVisible()) {
            this.setVisible(true);
            registerKeyListeners();
            if (rad instanceof RADVisualContainer<?>) {
                keyboardMenuNavigator.setCurrentMenuRAD((RADVisualContainer<?>) rad);
            } else {
                keyboardMenuNavigator.setCurrentMenuRAD((RADVisualContainer<?>) rad.getParentComponent());
            }
        }

        List<RADComponent<?>> path = new ArrayList<>();
        RADComponent<?> temp = rad.getParentComponent();
        while (true) {
            if (temp == null) {
                break;
            }
            path.add(temp);
            temp = temp.getParentComponent();
            if (!isMenuRelatedRADComponent(temp)) {
                break;
            }
        }

        // go backwards, top to bottom
        for (int i = path.size() - 1; i >= 0; i--) {
            RADComponent<?> r = path.get(i);
            Component c = formDesigner.getComponent(r);
            if (c instanceof JMenu) {
                showMenuPopup((JMenu) c);
            }
        }

    }

    private void showContextMenu(Point popupPos) {
        FormInspector inspector = FormInspector.getInstance();
        Node[] selectedNodes = inspector.getSelectedNodes();
        JPopupMenu popup = NodeOp.findContextMenu(selectedNodes);
        if (!this.isVisible()) {
            this.setVisible(true);
        }
        if (popup != null) {
            popup.show(this, popupPos.x, popupPos.y);
        }
    }

    // returns true if this is a menu container that should be highlighted if the component
    // tcomp is dragged over it.
    public boolean canHighlightContainer(RADVisualContainer<?> targetContainer, RADVisualComponent<?> tcomp) {
        Class<?> beanclass = tcomp.getBeanClass();
        if (targetContainer != null && targetContainer.isMenuComponent() && targetContainer.canAddComponent(beanclass)) {
            return true;
        }
        return false;
    }
    // is this rollover code still being used?
    // this turns on and off the rollover highlight as well as auto-opening the menu
    // if it is a menu
    private JComponent prevRollover = null;

    public void rolloverContainer(RADVisualContainer<?> targetContainer) {
        if (targetContainer == null && prevRollover != null) {
            clearRollover();
        }
        if (targetContainer != null) {
            Component cRollover = formDesigner.getComponent(targetContainer);
            JComponent rollover = (JComponent) cRollover;
            if (rollover != prevRollover) {
                clearRollover();
            }
            prevRollover = rollover;
            prevRollover.setBorder(new Border() {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setStroke(DropTargetLayer.DROP_TARGET_LINE_STROKE);
                    g2.setColor(DropTargetLayer.DROP_TARGET_COLOR);
                    g2.drawRect(x, y, width, height);
                }

                @Override
                public Insets getBorderInsets(Component c) {
                    return new Insets(2, 2, 2, 2);
                }

                @Override
                public boolean isBorderOpaque() {
                    return false;
                }
            });
            prevRollover.repaint();
            if (rollover instanceof JMenu) {
                formDesigner.openMenu(targetContainer);
            }
        }
    }

    public void clearRollover() {
        if (prevRollover == null) {
            return;
        }
        prevRollover.setBorder(BorderFactory.createEmptyBorder());
        prevRollover.repaint();
        prevRollover = null;
    }

    void addRadComponentToBefore(JComponent target, RADComponentCreator creator) {
        addRadComponentTo(target, 0, creator);
    }

    void addRadComponentToAfter(JComponent target, RADComponentCreator creator) {
        addRadComponentTo(target, 1, creator);
    }

    private void addRadComponentTo(JComponent target, int offset, RADComponentCreator creator) {
        try {
            JComponent targetParent = getMenuParent(target);
            if (target.getParent() instanceof JMenuBar) {
                targetParent = (JComponent) target.getParent();
            }
            RADVisualComponent<?> targetRad = (RADVisualComponent<?>) formDesigner.getRadComponent(target);
            RADVisualContainer<?> targetParentRad = (RADVisualContainer<?>) formDesigner.getRadComponent(targetParent);

            assert targetParentRad != null;

            int index2 = targetParentRad.getIndexOf(targetRad) + offset;
            creator.addPrecreatedComponent(targetParentRad, index2, null);
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
        }

    }

    boolean addRadComponentToEnd(JComponent targetComponent, RADComponentCreator creator) throws Exception {
        RADVisualContainer<?> targetContainer = (RADVisualContainer<?>) formDesigner.getRadComponent(targetComponent);
        boolean added = creator.addPrecreatedComponent(targetContainer, -1, null);
        return added;
    }

    void moveRadComponentInto(JComponent payload, JComponent targetMenu) {
        try {

            //check if dragging onto self
            if (payload == targetMenu) {
                return;
            }

            //check if dragging to a descendant node
            if (isAncestor(targetMenu, payload)) {
                return;
            }

            JComponent payloadParent = getMenuParent(payload);
            if (payloadParent == null) {
                payloadParent = (JComponent) payload.getParent();
            }
            RADVisualComponent<?> payloadRad = (RADVisualComponent<?>) formDesigner.getRadComponent(payload);
            RADVisualContainer<?> payloadParentRad = (RADVisualContainer<?>) formDesigner.getRadComponent(payloadParent);

            // remove the component from it's old location
            // if no payload rad then that probably means this is a new component from the palette
            if (payloadRad != null && payloadParentRad != null) {
                int index = payloadParentRad.getIndexOf(payloadRad);
                payloadParentRad.remove(payloadRad);
                formDesigner.getFormModel().fireComponentRemoved(payloadRad, payloadParentRad, index, false);
            }

            RADVisualContainer<?> targetMenuRad = (RADVisualContainer<?>) formDesigner.getRadComponent(targetMenu);
            //add inside the target menu
            //add to end of the toplevel menu
            targetMenuRad.add(payloadRad, -1);
            targetMenuRad.getLayoutSupport().addComponents(new RADVisualComponent<?>[]{payloadRad}, null, -1);
            formDesigner.getFormModel().fireComponentAdded(payloadRad, false);
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
        }
    }

    void moveRadComponentToBefore(JComponent payload, JComponent target) {
        moveRadComponentTo(payload, target, 0);
    }

    void moveRadComponentToAfter(JComponent payload, JComponent target) {
        moveRadComponentTo(payload, target, 1);
    }

    private void moveRadComponentTo(JComponent payload, JComponent target, int offset) {
        try {
            if (payload == target) {
                return;
            }
            //check if dragging to a descendant node
            if (isAncestor(target, payload)) {
                return;
            }
            JComponent payloadParent = getMenuParent(payload);
            JComponent targetParent = getMenuParent(target);
            if (targetParent == null) {
                targetParent = (JComponent) target.getParent();
            }
            RADVisualComponent<?> payloadRad = (RADVisualComponent<?>) formDesigner.getRadComponent(payload);
            RADVisualComponent<?> targetRad = (RADVisualComponent<?>) formDesigner.getRadComponent(target);
            RADVisualContainer<?> payloadParentRad = (RADVisualContainer<?>) formDesigner.getRadComponent(payloadParent);
            RADVisualContainer<?> targetParentRad = (RADVisualContainer<?>) formDesigner.getRadComponent(targetParent);

            //if a toplevel menu dragged next to another toplevel menu
            if (payload instanceof JMenu && payload.getParent() instanceof JMenuBar
                    && target instanceof JMenu && target.getParent() instanceof JMenuBar) {
                //remove from old spot
                targetParent = (JComponent) target.getParent();
                payloadParent = (JComponent) payload.getParent();
                payloadParentRad = (RADVisualContainer<?>) formDesigner.getRadComponent(payloadParent);
                targetParentRad = (RADVisualContainer<?>) formDesigner.getRadComponent(targetParent);
            }
            //skip if no payload rad, which probably means this is a new component from the palette
            if (payloadRad != null && payloadParentRad != null) {
                int index = payloadParentRad.getIndexOf(payloadRad);
                payloadParentRad.remove(payloadRad);
                formDesigner.getFormModel().fireComponentRemoved(payloadRad, payloadParentRad, index, false);
            }

            // only Menu component can be added into MenuBar, 
            // reset parent for the other components (issue #143248 fix)
            if (payloadRad != null
                    && !javax.swing.JMenu.class.isAssignableFrom(payloadRad.getBeanClass())
                    && target instanceof JMenu && targetParent instanceof JMenuBar) {
                targetParent = null;
            }

            //if dragged component into a toplevel menu
            if (targetParent == null && target instanceof JMenu && target.getParent() instanceof JMenuBar) {
                targetParentRad = (RADVisualContainer<?>) targetRad;
                //add to end of the toplevel menu
                targetParentRad.add(payloadRad, -1);
                targetParentRad.getLayoutSupport().addComponents(new RADVisualComponent<?>[]{payloadRad}, null, -1);
                formDesigner.getFormModel().fireComponentAdded(payloadRad, false);
                return;
            }

            // insert if target exists, else the item was removed by dragging out of the menu
            if (targetParentRad != null) {
                int index2 = targetParentRad.getIndexOf(targetRad) + offset;
                targetParentRad.add(payloadRad, index2);
                targetParentRad.getLayoutSupport().addComponents(new RADVisualComponent<?>[]{payloadRad},
                        null, index2);
                formDesigner.getFormModel().fireComponentAdded(payloadRad, false);
            }
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
        }
    }

    // change the look of the component to reflect the newly added state.
    // this mainly means making the foreground color light gray.
    void configureNewComponent(RADComponent<?> item) {
        if (item != null) {
            Component c = formDesigner.getComponent(item);
            if (c != null) {
                c.setForeground(Color.LIGHT_GRAY);
            }
        }
    }

    // change the look of the component to reflect the fully edited state
    private void configureEditedComponent(Component c) {
        if (c == null) {
            return;
        }
        if (USE_NEW_ITEM_COLOR_SWITCHING) {
            if (c.getForeground() == Color.LIGHT_GRAY) {
                c.setForeground(getNormalForeground(c));
            }
        }
    }

    void configureEditedComponent(RADComponent<?> c) {
        if (c != null) {
            configureEditedComponent(formDesigner.getComponent(c));
        }
    }

    //listens to see if this particular menu has been changed
    private void registerForm(final RADVisualContainer<?> radCont, final JMenu menu) {
        // don't double register
        if (!formModelListeners.containsKey(radCont)) {
            FormModelListener fml = new FormModelListener() {
                @Override
                public void formChanged(FormModelEvent[] events) {
                    if (events != null) {
                        for (FormModelEvent evt : events) {
                            if (evt.getChangeType() == FormModelEvent.FORM_TO_BE_CLOSED) {
                                formModelListeners.remove(radCont);
                                radCont.getFormModel().addFormModelListener(this);
                                continue;
                            }

                            if (evt.getChangeType() == FormModelEvent.COMPONENT_PROPERTY_CHANGED) {
                                if ("action".equals(evt.getPropertyName())) { // NOI18N
                                    configureEditedComponent(evt.getComponent());
                                }
                            }
                            if (evt.getChangeType() == FormModelEvent.COMPONENT_PROPERTY_CHANGED /*|| evt.getChangeType() == FormModelEvent.BINDING_PROPERTY_CHANGED*/) {
                                if (evt.getContainer() == radCont || evt.getComponent() == radCont) {
                                    rebuildOnScreenMenu(radCont);
                                }
                                updateIcon(evt.getComponent());
                            }

                            if (evt.getChangeType() == FormModelEvent.COMPONENT_ADDED) {
                                updateIcon(evt.getComponent());
                                //reinstall the accelerator preview when moving items around
                                if (evt.getComponent() != null) {
                                    Component co = formDesigner.getComponent(evt.getComponent());
                                    if (co instanceof JMenuItem) {
                                        installAcceleratorPreview((JMenuItem) co);
                                    }
                                }
                            }

                            // if this menu was deleted then make sure it's popup is hidden and removed
                            if (evt.getChangeType() == FormModelEvent.COMPONENT_REMOVED) {
                                if (evt.getComponent() == radCont) {
                                    unconfigureMenu(menu);
                                    continue;
                                }
                            }
                            // if something added to the menu we monitor
                            if (evt.getChangeType() == FormModelEvent.COMPONENT_ADDED
                                    || evt.getChangeType() == FormModelEvent.COMPONENTS_REORDERED
                                    || evt.getChangeType() == FormModelEvent.COMPONENT_REMOVED) {
                                if (evt.getContainer() == radCont) {
                                    // then rebuild the menu
                                    rebuildOnScreenMenu(radCont);
                                    return;
                                }
                                if (evt.getContainer() instanceof RADVisualContainer<?>) {
                                    RADVisualContainer<?> radContainer = (RADVisualContainer<?>) evt.getContainer();
                                    JComponent comp = formDesigner.getComponent(radContainer);
                                    if (comp instanceof JMenuBar) { // JMenuBar not shown in the designer, see issue 124873
                                        comp.removeAll();
                                        for (RADVisualComponent<?> c : radContainer.getSubComponents()) {
                                            if (c != null) {
                                                comp.add(formDesigner.getComponent(c));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            };
            formModelListeners.put(radCont, fml);
            radCont.getFormModel().addFormModelListener(fml);
        }
    }

    private void rebuildOnScreenMenu(RADVisualContainer<?> menuRAD) {
        if (menuRAD != null && hackedPopupFactory != null) {
            Component cMenu = formDesigner.getComponent(menuRAD);
            if (cMenu instanceof JMenu) {
                JMenu menu = (JMenu) cMenu;
                if (hackedPopupFactory.containerMap.containsKey(menu)) {
                    JPanel popupContainer = hackedPopupFactory.containerMap.get(menu);
                    if (popupContainer == null) {
                        return;
                    }
                    for (Component c : popupContainer.getComponents()) {
                        if (c instanceof JMenu) {
                            unconfigureMenu((JMenu) c);
                        } else {
                            unconfigureMenuItem((JComponent) c);
                        }
                    }
                    popupContainer.removeAll();
                    // rebuild it
                    for (RADVisualComponent<?> radChild : menuRAD.getSubComponents()) {
                        if (radChild != null) {
                            Component child = formDesigner.getComponent(radChild);
                            if (!isConfigured(child)) {
                                if (child instanceof JMenu) {
                                    configureMenu(menu, (JMenu) child);
                                } else {
                                    configureMenuItem(menu, child);
                                }
                            }
                            popupContainer.add(child);
                        }
                    }
                    // repack it
                    popupContainer.setSize(popupContainer.getLayout().preferredLayoutSize(popupContainer));
                    validate();
                    popupContainer.repaint();
                }
            }
        }
    }

    private void updateIcon(RADComponent<?> rad) {
        try {
            Component comp = formDesigner.getComponent(rad);
            if (comp instanceof MenuItem) {
                MenuItem item = (MenuItem) comp;
                RADProperty<?> icon_prop = rad.<RADProperty<?>>getProperty("icon");
                Object value = icon_prop.getValue();
                // extract the new value
                Icon icon = null;
                if (value instanceof Icon) {
                    icon = (Icon) value;
                }
                if (value instanceof NbImageIcon) {
                    icon = (NbImageIcon) value;
                }
                // do the actual update
                if (!(item.getIcon() instanceof WrapperIcon) && !isTopLevelMenu(item)) {
                    item.setIcon(new WrapperIcon(item.getIcon()));
                }

                if (item.getIcon() instanceof WrapperIcon) {
                    ((WrapperIcon) item.getIcon()).setIcon(icon);
                } else { // we should never get here
                    item.setIcon(icon);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException thr) {
            ErrorManager.getDefault().notify(thr);
        }
    }

    //returns true if this array contains a menu component
    public static boolean containsMenuTypeComponent(RADVisualComponent<?>[] comps) {
        if (comps == null) {
            return false;
        }
        if (comps.length < 1) {
            return false;
        }
        for (RADVisualComponent<?> c : comps) {
            if (JMenuItem.class.isAssignableFrom(c.getBeanClass())) {
                return true;
            }
            if (JMenu.class.isAssignableFrom(c.getBeanClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return True if this container is a menubar or menu, else false
     */
    public static boolean isValidMenuContainer(RADVisualContainer<?> cont) {
        if (cont == null) {
            return false;
        }
        if (JMenuBar.class.isAssignableFrom(cont.getBeanClass())) {
            return true;
        }
        if (JMenu.class.isAssignableFrom(cont.getBeanClass())) {
            return true;
        }
        return false;
    }

    public static boolean isTopLevelMenu(Component comp) {
        if (comp == null) {
            return false;
        }
        if (comp instanceof JMenu) {
            if (comp.getParent() instanceof JMenuBar) {
                return true;
            }
        }
        return false;
    }

    public boolean doesFormContainMenuBar() {
        return formDesigner.getFormModel().getAllComponents().stream().anyMatch((comp) -> (JMenuBar.class.isAssignableFrom(comp.getBeanClass())));
    }

    private class GlassLayerMouseListener extends MouseInputAdapter {

        Point pressPoint = null;
        Component pressComp = null;
        private boolean isEditing = false;

        @Override
        public void mousePressed(MouseEvent e) {
            try {
                //if this is a valid menu drop
                if (dragop.isStarted() && dragop.getTargetComponent() != null
                        && isMenuRelatedComponentClass(dragop.getTargetComponent().getClass())) {
                    if (e.isShiftDown()) {
                        dragop.end(e.getPoint(), false);
                        PaletteItem item = PaletteUtils.getSelectedItem();
                        dragop.start(item, e.getPoint());
                    } else {
                        dragop.end(e.getPoint(), true);
                    }
                    return;
                }
                if (shouldRedispatchToHandle()) {
                    dragop.fastEnd();
                    formDesigner.getHandleLayer().dispatchEvent(e);
                    return;
                }
                // drag drag ops
                if (dragop.isStarted()) {
                    dragop.end(e.getPoint());
                    return;
                }

                // open top level menus when clicking them
                RADVisualComponent<?> rad = formDesigner.getHandleLayer().getRadComponentAt(e.getPoint(), HandleLayer.COMP_DEEPEST);
                if (rad != null) {
                    Component c = formDesigner.getComponent(rad);
                    if (c != null && isTopLevelMenu(c)) {
                        if (e.getClickCount() > 1) {
                            isEditing = true;
                            configureEditedComponent(c);
                            formDesigner.startInPlaceEditing(rad);
                        } else {
                            openMenu(rad, c);
                            glassLayer.requestFocusInWindow();
                            if (DropTargetLayer.isMultiselectPressed(e)) {
                                addSelectedRADComponent(rad);
                            } else {
                                setSelectedRADComponent(rad);
                            }
                            if (e.isPopupTrigger()) {
                                showContextMenu(e.getPoint());
                                return;
                            }
                            if (!dragop.isStarted()) {
                                pressPoint = e.getPoint();
                                pressComp = c;
                                return;
                            }
                        }
                        return;
                    }
                    if (c instanceof JMenuBar) {
                        setSelectedRADComponent(rad);
                        if (e.isPopupTrigger()) {
                            showContextMenu(e.getPoint());
                            return;
                        }
                        return;
                    }
                }
                JComponent c = dragop.getDeepestComponentInPopups(e.getPoint());
                if (c == null && !isMenuRelatedRADComponent(rad)) {
                    PaletteUtils.clearPaletteSelection();
                    hideMenuLayer();
                    formDesigner.getHandleLayer().getMouseProcessor().mousePressed(e);
                    return;
                }
                // start editing
                if (e.getClickCount() > 1) {
                    if (c instanceof JMenuItem) {
                        JMenuItem item = (JMenuItem) c;
                        Point pt = SwingUtilities.convertPoint(glassLayer, e.getPoint(), item);
                        SelectedPortion portion = DropTargetLayer.calculateSelectedPortion(item, pt);
                        RADComponent<?> radcomp = formDesigner.getRadComponent(item);
                        configureEditedComponent(c);
                        if (portion == SelectedPortion.Icon) {
                            showIconEditor(radcomp);
                        } else if (portion == SelectedPortion.Accelerator) {
                            showAcceleratorEditor(radcomp);
                        } else {
                            isEditing = true;
                            formDesigner.startInPlaceEditing(radcomp);
                        }
                    }
                }

                // show context menu
                if (e.isPopupTrigger()) {
                    showContextMenu(e.getPoint());
                    return;
                }

                //prep for drag motion for menuitem to menuitem drags
                if (!dragop.isStarted() && c instanceof JMenuItem) {
                    pressPoint = e.getPoint();
                    pressComp = c;
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e.getPoint());
                return;
            }

            try {
                if (dragop.isStarted() && !e.isShiftDown()) {
                    dragop.end(e.getPoint());
                } else {
                    if (!isEditing) {
                        JComponent c = dragop.getDeepestComponentInPopups(e.getPoint());
                        if (c != null) {
                            if (c instanceof JMenuItem) {
                                Point localPt = SwingUtilities.convertPoint(glassLayer, e.getPoint(), c);
                                selectedPortion = DropTargetLayer.calculateSelectedPortion((JMenuItem) c, localPt);
                                dropTargetLayer.repaint();
                            } else {
                                selectedPortion = SelectedPortion.None;
                            }
                            glassLayer.requestFocusInWindow();
                            RADComponent<?> rad = formDesigner.getRadComponent(c);
                            assert rad instanceof RADVisualComponent<?>;
                            //add to selection if shift is down, instead of replacing
                            if (DropTargetLayer.isMultiselectPressed(e)) {
                                addSelectedRADComponent((RADVisualComponent<?>) rad);
                            } else {
                                setSelectedRADComponent((RADVisualComponent<?>) rad);
                            }
                        }
                    }
                    isEditing = false;
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }

        private void showIconEditor(RADComponent<?> comp) {
            try {
                RADProperty<?> prop = comp.<RADProperty<?>>getProperty("icon"); // NOI18N
                new PropertyAction(prop).actionPerformed(null);
            } catch (Throwable th) {
                ErrorManager.getDefault().notify(th);
            }
        }

        private void showAcceleratorEditor(RADComponent<?> comp) {
            try {
                RADProperty<?> prop = comp.<RADProperty<?>>getProperty("accelerator"); // NOI18N
                new PropertyAction(prop).actionPerformed(null);
            } catch (Throwable th) {
                ErrorManager.getDefault().notify(th);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (showMenubarWarning) {
                showMenubarWarning = false;
                repaint();
            }
            if (dragop.isStarted()) {
                if (PaletteUtils.getSelectedItem() == null && dragop.isPickAndPlop()) {
                    dragop.fastEnd();
                } else {
                    dragop.setTargetVisible(true);
                }
            }
            if (!dragop.isStarted() || PaletteUtils.getSelectedItem() != dragop.getCurrentItem()) {
                PaletteItem item = PaletteUtils.getSelectedItem();

                // if not menu related at all, then jump back to handle layer
                if (item != null && !isMenuRelatedComponentClass(item.getComponentClass())) {
                    hideMenuLayer();
                    return;
                }

                if (formDesigner.getDesignerMode() == PlatypusFormLayoutView.MODE_ADD && item != null) {
                    if (JMenuBar.class.isAssignableFrom(item.getComponentClass())) {
                        hideMenuLayer();
                        return;
                    }
                    dragop.start(item, e.getPoint());
                }

                /*
                 if(formDesigner.getDesignerMode() == PlatypusFormLayoutView.MODE_SELECT && showMenubarWarning) {
                 //glassLayer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                 showMenubarWarning = false;
                 repaint();
                 }*/
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (dragop.isStarted()) {
                dragop.setTargetVisible(false);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragop.isStarted() && pressPoint != null && pressComp instanceof JMenuItem
                    && e.getPoint().distance(pressPoint) > 10) {
                dragop.start((JMenuItem) pressComp, e.getPoint());
                pressPoint = null;
                pressComp = null;
            }
            if (dragop.isStarted()) {
                dragop.move(e.getPoint());
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (shouldRedispatchToHandle()) {
                formDesigner.getHandleLayer().dispatchEvent(e);
                //hideMenuLayer();
                //return;
            }
            if (dragop.isStarted()) {
                if (!doesFormContainMenuBar()) {
                    formDesigner.getFormModel().getAssistantModel().setContext("missingMenubar"); // NOI18N
                }
                dragop.move(e.getPoint());
            }

        }

        private boolean shouldRedispatchToHandle() {
            if (USE_JSEPARATOR_FIX && dragop.isStarted() && dragop.isPickAndPlop()) {
                if (dragop.getDragComponent() instanceof JSeparator /*&&
                         dropTargetLayer.getDropTargetComponent() == null*/) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean shouldRedispatchDnDToHandle(DropTargetDragEvent dtde) {
        RADComponent<?> rad = formDesigner.getHandleLayer().getRadComponentAt(dtde.getLocation(), HandleLayer.COMP_DEEPEST);
        if (rad != null && isMenuRelatedComponentClass(rad.getBeanClass())) {
            return false;
        }
        if (!USE_JSEPARATOR_FIX) {
            return false;
        }
        PaletteItem item = PaletteUtils.getSelectedItem();
        if (item != null && JSeparator.class.isAssignableFrom(item.getComponentClass())) {
            return true;
        }
        return false;
    }

    private class GlassLayerDropTargetListener implements DropTargetListener {

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            if (shouldRedispatchDnDToHandle(dtde)) {
                dragProxying = true;
                formDesigner.getHandleLayer().getNewComponentDropListener().dragEnter(dtde);
                return;
            }
            if (!dragop.isStarted()) {
                start(dtde);
            }
        }

        private void start(DropTargetDragEvent dtde) {
            PaletteItem item = PaletteUtils.getSelectedItem();

            if (item != null && !isMenuRelatedComponentClass(item.getComponentClass())) {
                hideMenuLayer();
                return;
            }

            if (formDesigner.getDesignerMode() == PlatypusFormLayoutView.MODE_ADD && item != null) {
                if (JMenuBar.class.isAssignableFrom(item.getComponentClass())) {
                    hideMenuLayer();
                    return;
                }
                dragop.start(item, dtde.getLocation());
            }
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            // look at the rad component under the cursor first
            if (dragProxying && shouldRedispatchDnDToHandle(dtde)) {
                formDesigner.getHandleLayer().getNewComponentDropListener().dragOver(dtde);
                return;
            }
            dragProxying = false;
            if (dragop.isStarted()) {
                dragop.move(dtde.getLocation());
            } else {
                start(dtde);
            }
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
            //if(shouldRedispatchDnDToHandle()) {
            if (dragProxying) {
                formDesigner.getHandleLayer().getNewComponentDropListener().dragExit(dte);
            }
            dragProxying = false;
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                //if(shouldRedispatchDnDToHandle()) {
                if (dragProxying) {
                    formDesigner.getHandleLayer().getNewComponentDropListener().drop(dtde);
                    dragProxying = false;
                    return;
                }
                if (dragop.isStarted()) {
                    dragop.end(dtde.getLocation());
                    dragProxying = false;
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
    }

    public SelectedPortion getCurrentSelectedPortion() {
        return selectedPortion;
    }
    private boolean dragProxying = false;

    public boolean isDragProxying() {
        return dragProxying;
    }

    static class WrapperIcon implements Icon {

        private Icon wrapee;

        public WrapperIcon() {
            this(null);
        }

        public WrapperIcon(Icon icon) {
            wrapee = icon;
        }

        public void setIcon(Icon icon) {
            this.wrapee = icon;
        }

        @Override
        public void paintIcon(Component arg0, Graphics g, int x, int y) {
            if (wrapee != null) {
                wrapee.paintIcon(arg0, g, x, y);
            } else {
                Graphics g2 = g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
                g2.setColor(MenuEditLayer.EMPTY_ICON_COLOR);
                g2.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
                g2.drawRect(x + 1, y + 1, getIconWidth() - 3, getIconHeight() - 3);
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            if (wrapee != null) {
                return wrapee.getIconWidth();
            }
            return 16;
        }

        @Override
        public int getIconHeight() {
            if (wrapee != null) {
                return wrapee.getIconHeight();
            }
            return 16;
        }
    }
}
