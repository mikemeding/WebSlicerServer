FILE(REMOVE_RECURSE
  "example_pb2.py"
  "example.pb.cc"
  "example.pb.h"
  "CMakeFiles/example.dir/example.cpp.o"
  "CMakeFiles/example.dir/example.pb.cc.o"
  "example.pdb"
  "example"
)

# Per-language clean rules from dependency scanning.
FOREACH(lang CXX)
  INCLUDE(CMakeFiles/example.dir/cmake_clean_${lang}.cmake OPTIONAL)
ENDFOREACH(lang)
