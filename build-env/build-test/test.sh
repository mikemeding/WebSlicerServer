#!/bin/bash
# CuraEngine slice [-v] [-p] [-j <settings.json>] [-s <settingkey>=<value>] [-g] [-e] [-o <output.gcode>] [-l <model.stl>]

CuraEngine slice -v -j prusa_i3.json -g -e -o "output/test.gcode" -l ../../models/test-models/balanced_die_version_2.stl
#CuraEngine slice -v -j ultimaker2.json -g -e -o "output/test.gcode" -l ../../models/ControlPanel.stl 
#CuraEngine slice -v -j maker_starter.json -g -e -o "output/test.gcode" -l ../../models/test-models/balanced_die_version_2.stl 
#CuraEngine slice -v -j machines/bq_hephestos_2.json -g -e -o "output/test.gcode" -l ../../models/test-models/balanced_die_version_2.stl
