#!/bin/bash
# CuraEngine slice [-v] [-p] [-j <settings.json>] [-s <settingkey>=<value>] [-g] [-e] [-o <output.gcode>] [-l <model.stl>]

../../CuraEngine-master/build/CuraEngine slice -v -j ultimaker2.json -g -e -o "output/test.gcode" -l ../../models/ControlPanel.stl 

