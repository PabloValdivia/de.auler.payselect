package de.aulersipel.component;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;
import org.compiere.util.CLogger; 

public class FormFactory implements IFormFactory{
	
	protected transient CLogger log = CLogger.getCLogger(getClass());
	
	@Override
	public ADForm newFormInstance(String formName) {
		if (formName.startsWith("de.aulersipel.form")){
			Object form = null;
			Class<?>clazz = null;
			ClassLoader loader = getClass().getClassLoader();
			try{
				clazz = loader.loadClass(formName);
				
			}catch (Exception e){
				log.fine("load FORM de.aulersipel.form FAILED" );
			}
			if (clazz!=null){
				try{
					form = clazz.newInstance();
				}catch (Exception e){
					log.fine("load FORM NEW INSTANCE de.aulersipel.form FAILED" );
				}
			}
			if (form!=null){
				if (form instanceof ADForm){
					return (ADForm)form;
				}else if (form instanceof IFormController){
					IFormController controller = (IFormController)form;
					ADForm adform = controller.getForm();
					adform.setICustomForm(controller);
					return adform;
				}
			}
		}
		return null;
	}

}
