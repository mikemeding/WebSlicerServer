#!/bin/bash
# CuraEngine slice [-v] [-p] [-j <settings.json>] [-s <settingkey>=<value>] [-g] [-e] [-o <output.gcode>] [-l <model.stl>]

./build/CuraEngine slice -v -j ../prusa_i3.json -g -e -o "output/test.gcode" -l ../raldrich_planetary.stl

