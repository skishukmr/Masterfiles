NOTES:

1) "brand/" contains AW Widgets / Wizard resources that you want to override for this app.

LAUNCHING:
        if current working directory is this source directory, then use:
            demoshell.sh sample/Home.htm

WIZARDS:
- Wizards are *directories* of with a ".awz" extension
- Step files should be names "StepX-Some_Name.htm"
        - the result will be a step named "Step Name"
- You should have an "Exit.htm" file to handle user exit actions.
