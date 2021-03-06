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

package com.jfoenix.responsive;

import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * @author sshahine
 * Responsive handler will scan all nodes in the scene and add a certain 
 * pseudo class (style class) to them according to the device ( screen size )
 * 
 */

public class JFXResponsiveHandler {
	
	public static final PseudoClass PSEUDO_CLASS_EX_SMALL = PseudoClass.getPseudoClass("extreme-small-device");
	public static final PseudoClass PSEUDO_CLASS_SMALL = PseudoClass.getPseudoClass("small-device");
	public static final PseudoClass PSEUDO_CLASS_MEDIUM = PseudoClass.getPseudoClass("medium-device");
	public static final PseudoClass PSEUDO_CLASS_LARGE = PseudoClass.getPseudoClass("large-device");
	
	
	public JFXResponsiveHandler(Stage stage, PseudoClass pseudoClass) {
		scanAllNodes(stage.getScene().getRoot(), PSEUDO_CLASS_LARGE);		
		
	}
	
	private void scanAllNodes(Parent parent, PseudoClass pseudoClass){		
		parent.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>(){
			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Node> c) {
				while (c.next())
					if (!c.wasPermutated() && !c.wasUpdated()) 
	                     for (Node addedNode : c.getAddedSubList()) 
	                    	 if(addedNode instanceof Parent)
	                    		 scanAllNodes((Parent) addedNode,pseudoClass);
			}
    	});		
		for (Node component : parent.getChildrenUnmodifiable()) {
	        if (component instanceof Pane) {
	        	((Pane)component).getChildren().addListener(new ListChangeListener<Node>(){
					@Override
					public void onChanged(javafx.collections.ListChangeListener.Change<? extends Node> c) {
						while (c.next()) {
							if (!c.wasPermutated() && !c.wasUpdated()) {
			                     for (Node addedNode : c.getAddedSubList()) {
			                    	 if(addedNode instanceof Parent)
			                    		 scanAllNodes((Parent) addedNode,pseudoClass);
			                     }
			                 }
						}
					}
	        	});
	            //if the component is a container, scan its children
	        	scanAllNodes((Pane) component, pseudoClass);
	        } else if (component instanceof ScrollPane){
	        	((ScrollPane)component).contentProperty().addListener((o,oldVal,newVal)-> {
	        		scanAllNodes((Parent) newVal,pseudoClass);	
	        	});
	            //if the component is a container, scan its children
	        	if(((ScrollPane)component).getContent() instanceof Parent){
	        		
	        		scanAllNodes((Parent) ((ScrollPane)component).getContent(), pseudoClass);
	        	}
	        } else if (component instanceof Control) {
	            //if the component is an instance of IInputControl, add to list	        	
	        	((Control)component).pseudoClassStateChanged(PSEUDO_CLASS_EX_SMALL, pseudoClass == PSEUDO_CLASS_EX_SMALL);
	        	((Control)component).pseudoClassStateChanged(PSEUDO_CLASS_SMALL, pseudoClass == PSEUDO_CLASS_SMALL);
	        	((Control)component).pseudoClassStateChanged(PSEUDO_CLASS_MEDIUM, pseudoClass == PSEUDO_CLASS_MEDIUM);
	        	((Control)component).pseudoClassStateChanged(PSEUDO_CLASS_LARGE, pseudoClass == PSEUDO_CLASS_LARGE);
	        }
	    }
	}
	
	
}
