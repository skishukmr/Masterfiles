/*
 * CatIntegrationPostLoadRole.java
 *
 * Trigger that  CatIntegrationPostLoadRole when CatInclusionsPull is run.
 *
 */
package config.java.action;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.user.core.Log;
import ariba.user.core.Role;
import ariba.util.core.Assert;
import ariba.util.core.PropertyTable;

public class CatIntegrationPostLoadRole extends Action {

    public void fire(ValueSource object, PropertyTable params) {
		Log.user.debug("%s : calling CatIntegrationPostLoadRole ", ClassName);

       Assert.that(object instanceof ariba.user.core.Role, "Tried to call a non Role object in %s", ClassName);
       Role role = (Role)object;

       // Update LastName
       String lastName = (String) role.getDottedFieldValue("Name.PrimaryString").toString();
       role.setFieldValue("LastName", lastName.toUpperCase());
    }

    public CatIntegrationPostLoadRole() { }
    public static final String ClassName = "CatIntegrationPostLoadRole";

}
