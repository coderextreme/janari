#!/bin/bash
# Run the ANARI JavaFX application with a green box (helide CPU ray tracer)
# This uses the locally built anari-java jars and the installed ANARI SDK

mkdir -p /tmp/anari-sdk/install
ln -s /home/yottzumm/janari/ANARI-SDK-0.16.0/build /tmp/anari-sdk/install/lib64

export LD_LIBRARY_PATH=/tmp/anari-sdk/install/lib64


# Use locally built jars from anari-java build
export CLASSPATH=anari-java/anari-binding/target/anari-binding-0.1.jar:anari-java/anari-example/target/anari-example-0.1.jar:anari-java/anari-javafx/target/anari-javafx-0.1.jar:/home/yottzumm/.m2/repository/org/openjfx/javafx-graphics/25.0.1/javafx-graphics-25.0.1.jar:/home/yottzumm/.m2/repository/org/openjfx/javafx-graphics/25.0.1/javafx-graphics-25.0.1-linux.jar:/home/yottzumm/.m2/repository/org/openjfx/javafx-base/25.0.1/javafx-base-25.0.1.jar:/home/yottzumm/.m2/repository/org/openjfx/javafx-base/25.0.1/javafx-base-25.0.1-linux.jar:/home/yottzumm/.m2/repository/org/openjfx/javafx-controls/25.0.1/javafx-controls-25.0.1.jar:/home/yottzumm/.m2/repository/org/openjfx/javafx-controls/25.0.1/javafx-controls-25.0.1-linux.jar:/home/yottzumm/X3DJSONLD/X3DJSAIL.4.0.full.jar

# Use helide (CPU ray tracing device) - works with Java bindings
export ANARI_LIBRARY=helide

# Run with NVIDIA offload for GPU acceleration (if available)
# -Dprism.order=sw forces software rendering for JavaFX (avoids GPU conflicts)
__NV_PRIME_RENDER_OFFLOAD=1 __GLX_VENDOR_LIBRARY_NAME=nvidia java \
    -Dprism.order=sw \
    -Djava.library.path=/tmp/anari-sdk/install/lib64 \
    --enable-native-access=ALL-UNNAMED \
    --add-modules=ALL-DEFAULT \
    -cp ${CLASSPATH} \
    JanariApp.java
# X3DApplication.java
