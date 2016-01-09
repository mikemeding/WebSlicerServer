# WebSlicer Server
To install,
unzip all items in build-env folder in the main folder and follow the directions for each below.

#CuraEngine
mkdir build && cd build
cmake ..
make

#Libraries
mkdir build && cd build
cmake ..
make
make install (root required)

#Check if working with 
build-env/build-test/test.sh


