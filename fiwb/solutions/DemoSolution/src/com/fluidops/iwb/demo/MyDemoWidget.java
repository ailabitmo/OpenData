package com.fluidops.iwb.demo;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FTextInput2;

import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.widget.AbstractWidget;


/**
 * On some wiki page add
 * 
 * <code>
 * = Test my demo widget =
 * 
 * <br/>
 * {{#widget: com.fluidops.iwb.widget.MyDemoWidget 
 * | labelText = 'Enter your name'
 * }}
 * 
 * </code>
 * 
 */
public class MyDemoWidget extends AbstractWidget<MyDemoWidget.Config> {
	
	public static class Config {		
		@ParameterConfigDoc(desc = "The types for which the wizard generates data")
                public String labelText;
	}

	@Override
	protected FComponent getComponent(String id) {

		Config config = get();
		
		config.labelText = config.labelText==null ? "Enter some text:" : config.labelText;
		
		// the layouting container for this widget
		// the container must use the provided id
		FContainer cnt = new FContainer(id);
		
		// now we can add other components to the container
		// the simplest is to add them line by line
		
		// 1) add a label with the labelText
		final FLabel label = new FLabel("label", config.labelText);
		cnt.add(label);	
		
		// 2) add a text field 
		final FTextInput2 input = new FTextInput2("inp");
		cnt.add(input);
		
		// 3) add two buttons next two each other
		// a) alert content of text field
		FButton btnOk = new FButton("btn_OK", "Alert input") {
			@Override
			public void onClick() {
				addClientUpdate(new FClientUpdate("alert('" + input.getValue() + "');"));				
			}
		};
		btnOk.appendClazz("floatLeft");
		cnt.add(btnOk);
		// b) cancel button to clear text input
		FButton btnCancel = new FButton("btn_Cancel", "Clear input") {
			@Override
			public void onClick() {
				input.setValueAndRefresh("");				
			}
		};
		cnt.add(btnCancel);
		
		// 4) button to trigger API call
		FButton triggerApi = new FButton("btn_Api", "Trigger Api") {
			@Override
			public void onClick() {
				// DemoApi.doSomething();			
			}
		};
		triggerApi.addStyle("margin-left", "50px");
		cnt.add(triggerApi);	
		
		return cnt;
	}	

	@Override
	public String getTitle() {
		return "My first widget";
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
}