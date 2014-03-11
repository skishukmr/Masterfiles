#
#   Copyright (c) 1996-2004 Ariba, Inc.
#   All rights reserved. Patents pending.
#
#   $Id$
#
#   Responsible: ariba
#
#

Catalog Category Image Installation Instructions:

-----------------------------
Enable category image feature
-----------------------------

Set parameter System.Catalog.Content.CategoryImageEnabled to true.
Set parameter System.Catalog.Content.ContentPath to a directory path 
under {Server}.

-------------------------------------
Installing the sample category images
-------------------------------------

This directory includes catalog category image examples.  To use
these sample images, just make sure they are under 
{Server}/{System.Catalog.Content.ContentPath}/CatalogHierarchyImages 
directory.

---------------------------
Changing a category's image
---------------------------

To change a category's image, log into ABA as the catalog manager. 
Go to Catalog Manager / Hierarchy Editor.  Left mouse click on the
category and select Image Info menu to change its category image.
The change should be saved to the system version of hierarchy file 
for it to take effect in catalog search ui.

------------------
Testing the images
------------------

The specified category image should be shown in both ABA Hierarchy
Editor image info screen and catalog search ui screen.

If a specified category image file is missing from
Server}/{ContentPath directory}/CatalogHierarchyImages directory, its parent
category image will be used.  If no parent category image is availble,
the default category image will be used.
