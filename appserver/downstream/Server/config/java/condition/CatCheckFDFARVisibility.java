/* Created by Deepak Sharma for CR159 - Oct 24 2008
 * Modified by Kanan CR 159 - Nov 5, 2008
 * ---------------------------------------------
 * 24.10.2008 (Deepak) - Created a visible condition for a newly created field.
 * 05.11.2008 (Kannan) - Added one more condition for visibility to show in the req header.

 */

package config.java.condition;
        Log.customer.debug("CatCheckFDFARVisibility FARDFARDepartment2 value from cat.vcsv1 .."+ dept2);
        Log.customer.debug("CatCheckFDFARVisibility FARDFARDepartment3 value from cat.vcsv1 .."+ dept3);


        //if (object instanceof BaseObject)  {

                Requisition req = (Requisition)object;

                                                  return true;
                                                }
                                             req.setFieldValue("FDFARAttachmentIndicator",Boolean.FALSE);
                                             return false;
                Boolean extnComment = null;
        return false;
}