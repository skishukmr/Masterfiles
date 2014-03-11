#
#   Copyright (c) 1996-2003 Ariba, Inc.
#   All rights reserved. Patents pending.
#
#   $Id: README.txt#1 $
#
#   Responsible: ariba
#
#

After installation this folder should be located at <install_dir>/config/resource/en_US/templates.

This folder consists of the out-of-box templates which will be shipped along with the HTML Email sample.
Under the email directory there various folders for different types of approvable types and each approvable
type folder consists of templates for various notification actions.

The mapping file [config/EmailSimpleTemplate.table] specifies which template to use for which approvable
type and notification action.

The folders and templates included are :

1. email/common
  - consists of all the templates which are standalone components and can be reused by including into
    other templates. These include header template, generic template, consolidated template etc.

2. email/requisition
  - all requistion specific templates

3. email/expense
  - all expense report specific templates

4. email/travel
  - all travel authorization specific templates

5. email/travelprof
  - all travel profile specific templates

6. email/contracts
  - all contracts specific templates



