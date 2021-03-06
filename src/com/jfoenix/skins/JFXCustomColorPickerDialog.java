/*
 * JFoenix
 * Copyright (c) 2015, JFoenix and/or its affiliates., All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.jfoenix.skins;

import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.effects.JFXDepthManager;
import com.jfoenix.svg.SVGGlyph;
import com.jfoenix.transitions.CachedTransition;
import com.jfoenix.transitions.JFXFillTransition;


/**
 * @author sshahine
 *
 */
public class JFXCustomColorPickerDialog  extends StackPane {

	private final Stage dialog = new Stage();

	private ObjectProperty<Color> currentColorProperty = new SimpleObjectProperty<>(Color.WHITE);
	private ObjectProperty<Color> customColorProperty = new SimpleObjectProperty<>(Color.TRANSPARENT);
	private Runnable onSave;
	private Runnable onUse;
	private Runnable onCancel;

	private Scene customScene;
	private JFXCustomColorPicker curvedColorPicker;
	private ParallelTransition paraTransition;
	private JFXDecorator pickerDecorator;
	private boolean systemChange = false;
	private boolean userChange = false;

	public JFXCustomColorPickerDialog(Window owner) {
		getStyleClass().add("custom-color-dialog");
		if (owner != null) dialog.initOwner(owner);
		dialog.setTitle(JFXColorPickerSkin.getString("customColorDialogTitle"));
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initStyle(StageStyle.TRANSPARENT);
		dialog.setResizable(false);

		// create JFX Decorator
		pickerDecorator = new JFXDecorator(dialog,this, false,false,false);
		pickerDecorator.setOnCloseButtonAction(()-> close());
		pickerDecorator.setPickOnBounds(false);
		JFXDepthManager.setDepth(pickerDecorator, 2);		
		StackPane decoratorContainer = new StackPane(pickerDecorator);
		decoratorContainer.setPadding(new Insets(20));
		decoratorContainer.setStyle("-fx-background-color:TRANSPARENT;");
		decoratorContainer.setPickOnBounds(false);		
		customScene = new Scene(decoratorContainer, Color.TRANSPARENT);
		final Scene ownerScene = owner.getScene();
		if (ownerScene != null) {
			if (ownerScene.getUserAgentStylesheet() != null) {
				customScene.setUserAgentStylesheet(ownerScene.getUserAgentStylesheet());
			}
			customScene.getStylesheets().addAll(ownerScene.getStylesheets());
		}
		curvedColorPicker = new JFXCustomColorPicker();   

		StackPane pane = new StackPane(curvedColorPicker);       
		pane.setPadding(new Insets(18));

		VBox container = new VBox();
		container.getChildren().add(pane);

		JFXTabPane tabs = new JFXTabPane();		

		JFXTextField rgbField = new JFXTextField();
		JFXTextField hsbField = new JFXTextField();
		JFXTextField hexField = new JFXTextField();


		rgbField.setStyle("-fx-background-color:TRANSPARENT;-fx-font-weight: BOLD;-fx-prompt-text-fill: #808080; -fx-alignment: top-left ; -fx-max-width: 300;");
		rgbField.setPromptText("RGB Color");
		rgbField.textProperty().addListener((o,oldVal,newVal)-> updateColorFromUserInput(newVal));

		hsbField.setStyle("-fx-background-color:TRANSPARENT;-fx-font-weight: BOLD;-fx-prompt-text-fill: #808080; -fx-alignment: top-left ; -fx-max-width: 300;");
		hsbField.setPromptText("HSB Color");		
		hsbField.textProperty().addListener((o,oldVal,newVal)-> updateColorFromUserInput(newVal));
		
		hexField.setStyle("-fx-background-color:TRANSPARENT;-fx-font-weight: BOLD;-fx-prompt-text-fill: #808080; -fx-alignment: top-left ; -fx-max-width: 300;");
		hexField.setPromptText("#HEX Color");
		hexField.textProperty().addListener((o,oldVal,newVal)-> updateColorFromUserInput(newVal));
		
		StackPane tabContent = new StackPane();
		tabContent.getChildren().add(rgbField);
		tabContent.setMinHeight(100);

		Tab rgbTab = new Tab("RGB");
		rgbTab.setContent(tabContent);
		Tab hsbTab = new Tab("HSB");
		hsbTab.setContent(hsbField);
		Tab hexTab = new Tab("HEX");
		hexTab.setContent(hexField);
		
		tabs.getTabs().add(rgbTab);		
		tabs.getTabs().add(hsbTab);		
		tabs.getTabs().add(hexTab);

		// change tabs labels font color according to the selected color
		pane.backgroundProperty().addListener((o,oldVal,newVal)->{			
			Color fontColor = ((Color)newVal.getFills().get(0).getFill()).grayscale().getRed() > 0.5? Color.valueOf("rgba(40, 40, 40, 0.87)") : Color.valueOf("rgba(255, 255, 255, 0.87)");						
			tabs.lookupAll(".tab").forEach(tabNode->tabNode.lookupAll(".tab-label").forEach(node-> ((Label)node).setTextFill(fontColor)));
			tabs.lookupAll(".tab").forEach(tabNode->tabNode.lookupAll(".jfx-rippler").forEach(node-> ((JFXRippler)node).setRipplerFill(fontColor)));
			((Line)tabs.lookup(".tab-selected-line")).setStroke(fontColor);
			pickerDecorator.lookupAll(".jfx-decorator-button").forEach(button->{
				((JFXButton)button).setRipplerFill(fontColor);
				((SVGGlyph)((JFXButton)button).getGraphic()).setFill(fontColor);
			});

			Color newColor = (Color) newVal.getFills().get(0).getFill();
			String hex = String.format("#%02X%02X%02X",
					(int)( newColor.getRed() * 255),
					(int)( newColor.getGreen() * 255),
					(int)( newColor.getBlue() * 255));
			String rgb = String.format("rgba(%d, %d, %d, 1)",
					(int)( newColor.getRed() * 255),
					(int)( newColor.getGreen() * 255),
					(int)( newColor.getBlue() * 255));
			String hsb = String.format("hsl(%d, %d%%, %d%%)",
					(int)( newColor.getHue()),
					(int)(newColor.getSaturation()*100),
					(int)(newColor.getBrightness()*100));

			if(!userChange){
				Platform.runLater(()->{
					systemChange = true;
					rgbField.setText(rgb);
					hsbField.setText(hsb);
					hexField.setText(hex);
					systemChange = false;	
				});
			}
		});


		curvedColorPicker.selectedPath.addListener((o,oldVal,newVal)->{
			if(paraTransition!=null) paraTransition.stop();
			Region tabsHeader = (Region) tabs.lookup(".tab-header-background");    
			pane.backgroundProperty().unbind();
			tabsHeader.backgroundProperty().unbind();
			JFXFillTransition fillTransition = new JFXFillTransition(Duration.millis(240), pane, (Color)oldVal.getFill(), (Color)newVal.getFill());
			JFXFillTransition tabsFillTransition = new JFXFillTransition(Duration.millis(240), tabsHeader, (Color)oldVal.getFill(), (Color)newVal.getFill());
			paraTransition = new ParallelTransition(fillTransition, tabsFillTransition);
			paraTransition.setOnFinished((finish)->{
				tabsHeader.backgroundProperty().bind(Bindings.createObjectBinding(()->{
					return new Background(new BackgroundFill(newVal.getFill(), CornerRadii.EMPTY, Insets.EMPTY));
				}, newVal.fillProperty()));
				pane.backgroundProperty().bind(Bindings.createObjectBinding(()->{
					return new Background(new BackgroundFill(newVal.getFill(), CornerRadii.EMPTY, Insets.EMPTY));
				}, newVal.fillProperty()));												
			});						
			paraTransition.play();			
		});

		// initial selected colors
		Platform.runLater(()->{			
			pane.setBackground(new Background(new BackgroundFill(curvedColorPicker.getColor(curvedColorPicker.getSelectedIndex()), CornerRadii.EMPTY, Insets.EMPTY)));
			((Region) tabs.lookup(".tab-header-background")).setBackground(new Background(new BackgroundFill(curvedColorPicker.getColor(curvedColorPicker.getSelectedIndex()), CornerRadii.EMPTY, Insets.EMPTY)));
			Region tabsHeader = (Region) tabs.lookup(".tab-header-background");    
			pane.backgroundProperty().unbind();
			tabsHeader.backgroundProperty().unbind();			
			tabsHeader.backgroundProperty().bind(Bindings.createObjectBinding(()->{
				return new Background(new BackgroundFill(curvedColorPicker.selectedPath.get().getFill(), CornerRadii.EMPTY, Insets.EMPTY));
			}, curvedColorPicker.selectedPath.get().fillProperty()));
			pane.backgroundProperty().bind(Bindings.createObjectBinding(()->{
				return new Background(new BackgroundFill(curvedColorPicker.selectedPath.get().getFill(), CornerRadii.EMPTY, Insets.EMPTY));
			}, curvedColorPicker.selectedPath.get().fillProperty()));

			// bind text field line color
			rgbField.focusColorProperty().bind(Bindings.createObjectBinding(()->{
				return pane.getBackground().getFills().get(0).getFill();
			}, pane.backgroundProperty()));
			hsbField.focusColorProperty().bind(Bindings.createObjectBinding(()->{
				return pane.getBackground().getFills().get(0).getFill();
			}, pane.backgroundProperty()));
			hexField.focusColorProperty().bind(Bindings.createObjectBinding(()->{
				return pane.getBackground().getFills().get(0).getFill();
			}, pane.backgroundProperty()));
			
			
			((Pane)pickerDecorator.lookup(".jfx-decorator-buttons-container")).backgroundProperty().bind(Bindings.createObjectBinding(()->{
				return new Background(new BackgroundFill((Color) pane.getBackground().getFills().get(0).getFill(), CornerRadii.EMPTY, Insets.EMPTY));
			}, pane.backgroundProperty()));
			
			((Pane)pickerDecorator.lookup(".jfx-decorator-content-container")).borderProperty().bind(Bindings.createObjectBinding(()->{
				return new Border(new BorderStroke((Color) pane.getBackground().getFills().get(0).getFill(), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 4, 4, 4)));
			}, pane.backgroundProperty()));
			

		});

		container.getChildren().add(tabs);

		this.getChildren().add(container);
		this.setPadding(new Insets(0));

		dialog.setScene(customScene);
		dialog.addEventHandler(KeyEvent.ANY, keyEventListener);
	}

	private void updateColorFromUserInput(String colorWebString) {
		if(!systemChange){
			userChange = true;
			try {
				curvedColorPicker.setColor(Color.valueOf(colorWebString));
			} catch (Exception e) {
				// if color is not valid then do nothing
			}
			userChange = false;
		}
	}

	private final EventHandler<KeyEvent> keyEventListener = e -> {
		switch (e.getCode()) {
		case ESCAPE :
			close();
			break;
		case ENTER:
			close();
			this.customColorProperty.set(curvedColorPicker.getColor(curvedColorPicker.getSelectedIndex()));
			this.onSave.run();
			break;
		default:
			break;
		}
	};

	private void close(){
		dialog.setScene(null);
		dialog.close();
	}

	public void setCurrentColor(Color currentColor) {
		this.currentColorProperty.set(currentColor);
	}

	Color getCurrentColor() {
		return currentColorProperty.get();
	}

	ObjectProperty<Color> customColorProperty() {
		return customColorProperty;
	}

	void setCustomColor(Color color) {
		customColorProperty.set(color);
	}

	Color getCustomColor() {
		return customColorProperty.get();
	}

	public Runnable getOnSave() {
		return onSave;
	}

	public void setOnSave(Runnable onSave) {
		this.onSave = onSave;
	}

	public Runnable getOnUse() {
		return onUse;
	}

	public void setOnUse(Runnable onUse) {
		this.onUse = onUse;
	}

	public Runnable getOnCancel() {
		return onCancel;
	}

	public void setOnCancel(Runnable onCancel) {
		this.onCancel = onCancel;
	}

	public void setOnHidden(EventHandler<WindowEvent> onHidden) {
		dialog.setOnHidden(onHidden);
	}

	Stage getDialog() {
		return dialog;
	}

	public void show() {		
		pickerDecorator.setOpacity(0);
		if (dialog.getOwner() != null) {
			// Workaround of RT-29871: Instead of just invoking fixPosition() 
			// here need to use listener that fixes dialog position once both
			// width and height are determined
			dialog.widthProperty().addListener(positionAdjuster);
			dialog.heightProperty().addListener(positionAdjuster);
			positionAdjuster.invalidated(null);
		}
		if (dialog.getScene() == null) dialog.setScene(customScene);
		//        colorRectPane.updateValues();
		curvedColorPicker.preAnimate();
		dialog.show();		

		CachedTransition showStage = new CachedTransition(pickerDecorator,new Timeline(new KeyFrame(Duration.millis(1000), new KeyValue(pickerDecorator.opacityProperty(), 1, Interpolator.EASE_BOTH)))){{
			this.setDelay(Duration.millis(0));
			this.setCycleDuration(Duration.millis(320));
		}};
		showStage.setOnFinished((finish)-> curvedColorPicker.animate());
		showStage.play();
	}

	private InvalidationListener positionAdjuster = new InvalidationListener() {

		@Override
		public void invalidated(Observable ignored) {
			if (Double.isNaN(dialog.getWidth()) || Double.isNaN(dialog.getHeight())) {
				return;
			}
			dialog.widthProperty().removeListener(positionAdjuster);
			dialog.heightProperty().removeListener(positionAdjuster);
			fixPosition();
		}

	};


	private void fixPosition() {
		Window w = dialog.getOwner();
		Screen s = getScreen(w);
		Rectangle2D sb = s.getBounds();
		double xR = w.getX() + w.getWidth();
		double xL = w.getX() - dialog.getWidth();
		double x, y;
		if (sb.getMaxX() >= xR + dialog.getWidth()) {
			x = xR;
		} else if (sb.getMinX() <= xL) {
			x = xL;
		} else {
			x = Math.max(sb.getMinX(), sb.getMaxX() - dialog.getWidth());
		}
		y = Math.max(sb.getMinY(), Math.min(sb.getMaxY() - dialog.getHeight(), w.getY()));
		dialog.setX(x);
		dialog.setY(y);
	}

	@Override public void layoutChildren() {
		super.layoutChildren();
		if (dialog.getMinWidth() > 0 && dialog.getMinHeight() > 0) {
			// don't recalculate min size once it's set
			return;
		}

		// Math.max(0, ...) added for RT-34704 to ensure the dialog is at least 0 x 0
		double minWidth = Math.max(0, computeMinWidth(getHeight()) + (dialog.getWidth() - customScene.getWidth()));
		double minHeight = Math.max(0, computeMinHeight(getWidth()) + (dialog.getHeight() - customScene.getHeight()));
		dialog.setMinWidth(minWidth);
		dialog.setMinHeight(minHeight);
	}

	/*
	 * Added Private Java Methods for java version (1.8_u60) compatibility
	 */
    private static Screen getScreen(Object obj) {
        final Bounds parentBounds = getBounds(obj);

        final Rectangle2D rect = new Rectangle2D(
                parentBounds.getMinX(),
                parentBounds.getMinY(),
                parentBounds.getWidth(),
                parentBounds.getHeight());

        return getScreenForRectangle(rect);
    }

    private static Screen getScreenForRectangle(final Rectangle2D rect) {
        final List<Screen> screens = Screen.getScreens();

        final double rectX0 = rect.getMinX();
        final double rectX1 = rect.getMaxX();
        final double rectY0 = rect.getMinY();
        final double rectY1 = rect.getMaxY();

        Screen selectedScreen;

        selectedScreen = null;
        double maxIntersection = 0;
        for (final Screen screen: screens) {
            final Rectangle2D screenBounds = screen.getBounds();
            final double intersection =
                    getIntersectionLength(rectX0, rectX1,
                                          screenBounds.getMinX(),
                                          screenBounds.getMaxX())
                        * getIntersectionLength(rectY0, rectY1,
                                                screenBounds.getMinY(),
                                                screenBounds.getMaxY());

            if (maxIntersection < intersection) {
                maxIntersection = intersection;
                selectedScreen = screen;
            }
        }

        if (selectedScreen != null) {
            return selectedScreen;
        }

        selectedScreen = Screen.getPrimary();
        double minDistance = Double.MAX_VALUE;
        for (final Screen screen: screens) {
            final Rectangle2D screenBounds = screen.getBounds();
            final double dx = getOuterDistance(rectX0, rectX1,
                                               screenBounds.getMinX(),
                                               screenBounds.getMaxX());
            final double dy = getOuterDistance(rectY0, rectY1,
                                               screenBounds.getMinY(),
                                               screenBounds.getMaxY());
            final double distance = dx * dx + dy * dy;

            if (minDistance > distance) {
                minDistance = distance;
                selectedScreen = screen;
            }
        }

        return selectedScreen;
    }
    private static double getOuterDistance(
            final double a0, final double a1,
            final double b0, final double b1) {
        // (a0 <= a1) && (b0 <= b1)
        if (a1 <= b0) {
            return b0 - a1;
        }
        if (b1 <= a0) {
            return b1 - a0;
        }
        return 0;
    }
    private static Bounds getBounds(Object obj) {
        if (obj instanceof Node) {
            final Node n = (Node)obj;
            Bounds b = n.localToScreen(n.getLayoutBounds());
            return b != null ? b : new BoundingBox(0, 0, 0, 0);
        } else if (obj instanceof Window) {
            final Window window = (Window)obj;
            return new BoundingBox(window.getX(), window.getY(), window.getWidth(), window.getHeight());
        } else {
            return new BoundingBox(0, 0, 0, 0);
        }
    }
    private static double getIntersectionLength(
            final double a0, final double a1,
            final double b0, final double b1) {
        // (a0 <= a1) && (b0 <= b1)
        return (a0 <= b0) ? getIntersectionLengthImpl(b0, b1, a1)
                          : getIntersectionLengthImpl(a0, a1, b1);
    }

    private static double getIntersectionLengthImpl(
            final double v0, final double v1, final double v) {
        // (v0 <= v1)
        if (v <= v0) {
            return 0;
        }

        return (v <= v1) ? v - v0 : v1 - v0;
    }
	

}
