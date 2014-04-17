/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bearsoft.gwt.ui.widgets.grid.cells;

import java.util.Date;

import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.dom.client.Style;

/**
 * 
 * @author mg
 */
public abstract class RenderedPopupEditorCell<T> extends AbstractPopupEditorCell<T> {

	public static int CELL_PADDING = 2;
	
	protected interface Padded extends SafeHtmlTemplates {

		public static Padded INSTANCE = GWT.create(Padded.class);

		@Template("<div style='{0} position: relative; box-sizing: border-box; height: 100%; width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap'>{1}</div>")
		public SafeHtml get(SafeStyles aStyle, SafeHtml aContent);
	}

	protected CellRenderer<T> renderer;
	protected CellHasReadonly readonly;

	public RenderedPopupEditorCell(Widget aEditor) {
		super(aEditor, BrowserEvents.CLICK, /* BrowserEvents.DBLCLICK, */BrowserEvents.KEYUP);
	}

	public CellRenderer<T> getRenderer() {
		return renderer;
	}

	public void setRenderer(CellRenderer<T> aRenderer) {
		renderer = aRenderer;
	}

	public CellHasReadonly getReadonly() {
		return readonly;
	}

	public void setReadonly(CellHasReadonly readonly) {
		this.readonly = readonly;
	}

	@Override
	public void render(Context context, T value, SafeHtmlBuilder sb) {
		if (renderer == null || !renderer.render(context, value, sb)) {
			SafeHtmlBuilder content = new SafeHtmlBuilder();
			renderCell(context, value, content);
			sb.append(Padded.INSTANCE.get(new SafeStylesBuilder().padding(CELL_PADDING, Style.Unit.PX).toSafeStyles(), content.toSafeHtml()));
		}
	}

	public void startEditing(Element parent, T value, com.google.gwt.cell.client.ValueUpdater<T> valueUpdater, Runnable onEditorClose) {
		if (readonly == null || !readonly.isReadonly())
			super.startEditing(parent, value, valueUpdater, onEditorClose);
	}

	protected abstract void renderCell(Context context, T value, SafeHtmlBuilder sb);

	protected long clickTimestamp = -1;

	@Override
	public void onBrowserEvent(Context context, Element parent, T value, NativeEvent event, ValueUpdater<T> valueUpdater) {
		switch (event.getType()) {
		case BrowserEvents.CLICK:
			long newClickTimestamp = new Date().getTime();
			try {
				if (newClickTimestamp - clickTimestamp < 550 && !isEditing(context, parent, value)) {
					EventTarget et = event.getEventTarget();
					if (Element.is(et)) {
						final Element focused = Element.as(et);
						focused.blur();
						startEditing(parent, value, valueUpdater, new Runnable() {
							@Override
							public void run() {
								focused.focus();
							}

						});
					}
				} else {
					super.onBrowserEvent(context, parent, value, event, valueUpdater);
				}
			} finally {
				clickTimestamp = newClickTimestamp;
			}
			break;
		case BrowserEvents.KEYUP:
			if (event.getKeyCode() == KeyCodes.KEY_ENTER || event.getKeyCode() == KeyCodes.KEY_F2) {
				EventTarget et = event.getEventTarget();
				if (Element.is(et)) {
					final Element focused = Element.as(et);
					focused.blur();
					startEditing(parent, value, valueUpdater, new Runnable() {
						@Override
						public void run() {
							focused.focus();
						}

					});
				}
			} else {
				super.onBrowserEvent(context, parent, value, event, valueUpdater);
			}
			break;
		default:
			super.onBrowserEvent(context, parent, value, event, valueUpdater);
			break;
		}
	}
}
