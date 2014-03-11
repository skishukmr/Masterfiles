*** Which aml file do I modify?

If you are making a change to a field that is not Adapter specific, then you 
would make an extension in config/variants/Plain/extensions, and you won't
have to modify the Adapter specific files.

If you make a change to Cost Center, in the Oracle*Ext.aml files, you must also 
make the change in the corresponding SAP*Ext.aml file.

*** What if I add or remove a new dynamic class?

Most new dynamic classes should go in the common .aml files.

If the dynamic class is an Adapter specific file, then the change should go in 
the Adapter specific .aml file. 

Note that these changes should be done for files in the config/ directory and
not in the ariba/ directory.