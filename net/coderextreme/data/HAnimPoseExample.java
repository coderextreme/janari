package net.coderextreme.data;
import org.web3d.x3d.jsail.*;
import org.web3d.x3d.jsail.CADGeometry.*;
import org.web3d.x3d.jsail.Core.*;
import org.web3d.x3d.jsail.CubeMapTexturing.*;
import org.web3d.x3d.jsail.DIS.*;
import org.web3d.x3d.jsail.EnvironmentalEffects.*;
import org.web3d.x3d.jsail.EnvironmentalSensor.*;
import org.web3d.x3d.jsail.EventUtilities.*;
import org.web3d.x3d.jsail.Followers.*;
import org.web3d.x3d.jsail.Geometry2D.*;
import org.web3d.x3d.jsail.Geometry3D.*;
import org.web3d.x3d.jsail.Geospatial.*;
import org.web3d.x3d.jsail.Grouping.*;
import org.web3d.x3d.jsail.HAnim.*;
import org.web3d.x3d.jsail.Interpolation.OrientationInterpolator;
import org.web3d.x3d.jsail.Interpolation.*;
import org.web3d.x3d.jsail.KeyDeviceSensor.*;
import org.web3d.x3d.jsail.Layering.*;
import org.web3d.x3d.jsail.Layout.*;
import org.web3d.x3d.jsail.Lighting.*;
import org.web3d.x3d.jsail.NURBS.*;
import org.web3d.x3d.jsail.Navigation.*;
import org.web3d.x3d.jsail.Networking.*;
import org.web3d.x3d.jsail.ParticleSystems.*;
import org.web3d.x3d.jsail.Picking.*;
import org.web3d.x3d.jsail.PointingDeviceSensor.*;
import org.web3d.x3d.jsail.Rendering.*;
import org.web3d.x3d.jsail.RigidBodyPhysics.*;
import org.web3d.x3d.jsail.Scripting.*;
import org.web3d.x3d.jsail.Shaders.*;
import org.web3d.x3d.jsail.Shape.*;
import org.web3d.x3d.jsail.Sound.*;
import org.web3d.x3d.jsail.Text.*;
import org.web3d.x3d.jsail.Texturing3D.*;
import org.web3d.x3d.jsail.Texturing.*;
import org.web3d.x3d.jsail.Time.*;
import org.web3d.x3d.jsail.VolumeRendering.*;
import org.web3d.x3d.jsail.fields.*;
import java.util.ArrayList;
import java.util.List;
import net.coderextreme.X3DRoots;
public class HAnimPoseExample implements X3DRoots {
  public static void main(String[] args) {
    ConfigurationProperties.setXsltEngine(ConfigurationProperties.XSLT_ENGINE_NATIVE_JAVA);
    ConfigurationProperties.setDeleteIntermediateFiles(false);
    ConfigurationProperties.setStripTrailingZeroes(true);
    ConfigurationProperties.setStripDefaultAttributes(true);
    X3D model = new HAnimPoseExample().getRootNodeList().get(0); // only get one root node
    System.out.print(model.validationReport().trim());
    model.toFileX3D("../data/HAnimPoseExample.new.java.x3d");
    model.toFileJSON("../data/HAnimPoseExample.new.java.x3dj");
    }
    public List<X3D> getRootNodeList() {
    	List<X3D> list = new ArrayList<X3D>(1);
    	list.add(initialize());
    	return list;
    }
    public X3D initialize() {
      X3D X3D0 =  new X3D().setProfile(new SFString("Full")).setVersion(new SFString("4.0"))
      .setHead(new head()
        .addMeta(new meta().setName(new SFString("title")).setContent(new SFString("HAnimPoseExample.x3d")))
        .addMeta(new meta().setName(new SFString("description")).setContent(new SFString("Native XML definition of an experimental new node to simply capture a single pose for an HAnimHumanoid model. Expected usage is to allow HAnimHumanoid to contain multiple Pose nodes which can be activated and composed.")))
        .addMeta(new meta().setName(new SFString("created")).setContent(new SFString("11 December 2025")))
        .addMeta(new meta().setName(new SFString("modified")).setContent(new SFString("14 December 2025")))
        .addMeta(new meta().setName(new SFString("creator")).setContent(new SFString("Don Brutzman")))
        .addMeta(new meta().setName(new SFString("warning")).setContent(new SFString("under development for X3D 4.1")))
        .addMeta(new meta().setName(new SFString("specificationSection")).setContent(new SFString("HAnim Architecture volume 1 version 2.1 draft, clause 6 Object interfaces, section 6.4 Pose")))
        .addMeta(new meta().setName(new SFString("specificationUrl")).setContent(new SFString("https://www.web3d.org/specifications/X3Dv4Draft/ISO-IEC19774/ISO-IEC19774-1/ISO-IEC19774-1v2.1/ISO-IEC19774-1v2.1-WD/Architecture/ObjectInterfaces.html#Pose")))
        .addMeta(new meta().setName(new SFString("specificationSection")).setContent(new SFString("HAnim Architecture volume 1 version 2.1 draft, clause 4 Concepts, section 4.8.2 Modelling of human-like HAnim figures")))
        .addMeta(new meta().setName(new SFString("specificationUrl")).setContent(new SFString("https://www.web3d.org/specifications/X3Dv4Draft/ISO-IEC19774/ISO-IEC19774-1/ISO-IEC19774-1v2.1/ISO-IEC19774-1v2.1-WD/Architecture/concepts.html#ModellingHumanLikeHAnimFigures")))
        .addMeta(new meta().setName(new SFString("specificationSection")).setContent(new SFString("HAnim Architecture volume 1 version 2.1 draft, clause 4 Concepts, section 4.8.3 Poses")))
        .addMeta(new meta().setName(new SFString("specificationUrl")).setContent(new SFString("https://www.web3d.org/specifications/X3Dv4Draft/ISO-IEC19774/ISO-IEC19774-1/ISO-IEC19774-1v2.1/ISO-IEC19774-1v2.1-WD/Architecture/concepts.html#Poses")))
        .addMeta(new meta().setName(new SFString("generator")).setContent(new SFString("X3D-Edit 4.0, https://www.web3d.org/x3d/tools/X3D-Edit")))
        .addMeta(new meta().setName(new SFString("identifier")).setContent(new SFString("https://www.web3d.org/x3d/content/examples/HumanoidAnimation/Poses/HAnimPoseExample.x3d")))
        .addMeta(new meta().setName(new SFString("license")).setContent(new SFString("https://www.web3d.org/x3d/content/examples/license.html"))))
      .setScene(new Scene()
        .addChild(new WorldInfo().setDEF(new SFString("ModelInfo")).setInfo(new MFString0().getArray()).setTitle(new SFString("HAnimPoseExample.x3d")))
        .addChild(new Background().setSkyColor(new MFColor1().getArray()))
        .addChild(new NavigationInfo())
        .addChild(new Group().setDEF(new SFString("HandleInlineLoading"))
          .addComments(new CommentsBlock("Multiple HAnimHumanoid Inline/IMPORT models are available to support testing: Characters/ JinLOA1 JinLOA2 JinLOA3 JinLOA4 ../Skin/JoeKick ../Skin/JoeSkeletonSkinSite ../Skin/BoxMan1 ../Skin/BoxMan2"))
          .addComments(new CommentsBlock("Also tested satisfactorily: KoreanCharacter01Jin KoreanCharacter02Chul KoreanCharacter03Hyun KoreanCharacter04Young KoreanCharacter05Ju KoreanCharacter06Ga KoreanCharacter07No KoreanCharacter08Da KoreanCharacter09Ru KoreanCharacter10Mi KoreanCharacter11Min KoreanCharacter12Sun"))
          .addChild(new Inline().setDEF(new SFString("HumanoidInline")).setDescription(new SFString("remote HAnimHumanoid for IMPORT")).setUrl(new MFString2().getArray()))
          .addComments(new CommentsBlock("Note that the following importedDEF must match the EXPORT name found in remote file"))
          .addChild(new IMPORT().setAS(new SFString("HumanoidImported")).setImportedDEF(new SFString("JoeSkeletonSkinSite")).setInlineDEF(new SFString("HumanoidInline")))
          .addChild(new LoadSensor().setDEF(new SFString("HumanoidInlineLoadSensor")).setTimeOut(2d)
            .addChild(new Inline().setUSE(new SFString("HumanoidInline")))))
        .addChild(new Viewpoint().setDescription(new SFString("HAnimPose for HumanoidInline IMPORT model")).setPosition(new float[] {0f ,1f ,4f }))
        .addComments(new CommentsBlock("no longer required: including full model <HAnimHumanoid DEF='hanim_JinLOA1' loa='2' name='JinLOA1' scale='0.0225 0.0225 0.0225'> etc..."))
        .addChild(new Group().setDEF(new SFString("InterfaceButtonsGroup"))
          .addChild(new Transform().setDEF(new SFString("DisplayHeader")).setTranslation(new float[] {0f ,2f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString3().getArray())
                .setFontStyle(new FontStyle().setDEF(new SFString("HeaderFont")).setFamily(new MFString4().getArray()).setJustify(new MFString5().getArray()).setSize(0.15f ).setStyle(new SFString("BOLD"))))
              .setAppearance(new Appearance().setDEF(new SFString("PoseTextAppearance"))
                .setMaterial(new Material().setDiffuseColor(new float[] {0.1f ,0.5f ,0.3f })))))
          .addChild(new Transform().setDEF(new SFString("T_PoseInterface")).setTranslation(new float[] {-1.5f ,1.5f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString6().getArray())
                .setFontStyle(new FontStyle().setDEF(new SFString("SharedFont")).setFamily(new MFString7().getArray()).setJustify(new MFString8().getArray()).setSize(0.1f ).setStyle(new SFString("BOLD"))))
              .setAppearance(new Appearance().setUSE(new SFString("PoseTextAppearance"))))
            .addChild(new Shape()
              .setAppearance(new Appearance().setDEF(new SFString("TransparentAppearance"))
                .setMaterial(new Material().setTransparency(0.8f )))
              .setGeometry(new Box().setSize(new float[] {0.45f ,0.2f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("T_PoseTouchSensor")).setDescription(new SFString("select to move shoulders to \"T\" pose, leave other joints unchanged")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("T_PoseTouchSensor")).setToField(new SFString("commencePose")).setToNode(new SFString("T_Pose"))))
          .addChild(new Transform().setDEF(new SFString("A_PoseInterface")).setTranslation(new float[] {-1.5f ,1f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString9().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setUSE(new SFString("PoseTextAppearance"))))
            .addChild(new Shape()
              .addComments(new CommentsBlock("Selectable Text transparent Box for easy user selection"))
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.45f ,0.2f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("A_PoseTouchSensor")).setDescription(new SFString("select to move shoulders to \"A\" pose, leave other joints unchanged")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("A_PoseTouchSensor")).setToField(new SFString("commencePose")).setToNode(new SFString("A_Pose"))))
          .addChild(new Transform().setDEF(new SFString("TouchDown_PoseInterface")).setTranslation(new float[] {-1.5f ,0.5f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString10().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setUSE(new SFString("PoseTextAppearance"))))
            .addChild(new Shape()
              .addComments(new CommentsBlock("Selectable Text transparent Box for easy user selection"))
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.85f ,0.2f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("TouchDown_PoseTouchSensor")).setDescription(new SFString("select to transition all joints to TouchDown pose")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("TouchDown_PoseTouchSensor")).setToField(new SFString("commencePose")).setToNode(new SFString("TouchDown_Pose"))))
          .addChild(new Transform().setDEF(new SFString("I_PoseInterface")).setTranslation(new float[] {-1.5f ,0f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString11().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setUSE(new SFString("PoseTextAppearance"))))
            .addChild(new Shape()
              .addComments(new CommentsBlock("Selectable Text transparent Box for easy user selection"))
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.45f ,0.2f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("I_PoseTouchSensor")).setDescription(new SFString("select to transition all joints to \"I\" pose")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("I_PoseTouchSensor")).setToField(new SFString("commencePose")).setToNode(new SFString("I_Pose"))))
          .addChild(new Transform().setDEF(new SFString("FaceLeftPoseInterface")).setTranslation(new float[] {1.5f ,1.5f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString12().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setUSE(new SFString("PoseTextAppearance"))))
            .addChild(new Shape()
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.9f ,0.2f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("FaceLeftTouchSensor")).setDescription(new SFString("select to rotate body and Face Left, leave other joints unchanged")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("FaceLeftTouchSensor")).setToField(new SFString("commencePose")).setToNode(new SFString("FaceLeft_Pose"))))
          .addChild(new Transform().setDEF(new SFString("FaceRightPoseInterface")).setTranslation(new float[] {1.5f ,1f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString13().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setUSE(new SFString("PoseTextAppearance"))))
            .addChild(new Shape()
              .addComments(new CommentsBlock("Selectable Text transparent Box for easy user selection"))
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.9f ,0.2f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("FaceRightTouchSensor")).setDescription(new SFString("select to rotate body and Face Right, leave other joints unchanged")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("FaceRightTouchSensor")).setToField(new SFString("commencePose")).setToNode(new SFString("FaceRight_Pose"))))
          .addChild(new Transform().setDEF(new SFString("AnimatePosesInterface")).setTranslation(new float[] {1.5f ,0.5f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString14().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setDEF(new SFString("AnimationTextAppearance"))
                .setMaterial(new Material().setDiffuseColor(new float[] {0.1f ,0.2f ,0.3f }))))
            .addChild(new Shape()
              .addComments(new CommentsBlock("Selectable Text transparent Box for easy user selection"))
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.9f ,0.25f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("AnimatePosesTouchSensor")).setDescription(new SFString("select to animate current pose to \"I\" pose, then back to original pose, using TimeSensor events")))
            .addComments(new CommentsBlock("cycleInterval=4 also hard-coded in script execution message"))
            .addChild(new TimeSensor().setDEF(new SFString("AnimatePosesClock")).setCycleInterval(4d).setDescription(new SFString("directly animate several poses")))
            .addChild(new ScalarInterpolator().setDEF(new SFString("AnimatePosesLoopInterpolator")).setKey(new MFFloat15().getArray()).setKeyValue(new MFFloat16().getArray()))
            .addChild(new ROUTE().setFromField(new SFString("touchTime")).setFromNode(new SFString("AnimatePosesTouchSensor")).setToField(new SFString("startTime")).setToNode(new SFString("AnimatePosesClock")))
            .addChild(new ROUTE().setFromField(new SFString("fraction_changed")).setFromNode(new SFString("AnimatePosesClock")).setToField(new SFString("set_fraction")).setToNode(new SFString("AnimatePosesLoopInterpolator")))
            .addChild(new ROUTE().setFromField(new SFString("value_changed")).setFromNode(new SFString("AnimatePosesLoopInterpolator")).setToField(new SFString("set_fraction")).setToNode(new SFString("I_Pose"))))
          .addChild(new Transform().setDEF(new SFString("ResetDefaultPoseInterface")).setTranslation(new float[] {1.5f ,0f ,0f })
            .addChild(new Shape()
              .setGeometry(new Text().setString(new MFString17().getArray())
                .setFontStyle(new FontStyle().setUSE(new SFString("SharedFont"))))
              .setAppearance(new Appearance().setUSE(new SFString("AnimationTextAppearance"))))
            .addChild(new Shape()
              .addComments(new CommentsBlock("Selectable Text transparent Box for easy user selection"))
              .setAppearance(new Appearance().setUSE(new SFString("TransparentAppearance")))
              .setGeometry(new Box().setSize(new float[] {0.9f ,0.25f ,0.001f })))
            .addChild(new TouchSensor().setDEF(new SFString("ResetPoseTouchSensor")).setDescription(new SFString("select to immediately Rezero All Joints (to default \"I\" Pose) by sending resetAllJoints event")))
            .addChild(new ROUTE().setFromField(new SFString("isActive")).setFromNode(new SFString("ResetPoseTouchSensor")).setToField(new SFString("resetAllJoints")).setToNode(new SFString("FaceLeft_Pose")))))
        .addChild(new Group().setDEF(new SFString("HandleInlineLoadsensorRouting"))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("A_Pose")))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("H_Pose")))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("I_Pose")))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("T_Pose")))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("FaceLeft_Pose")))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("FaceRight_Pose")))
          .addChild(new ROUTE().setFromField(new SFString("isLoaded")).setFromNode(new SFString("HumanoidInlineLoadSensor")).setToField(new SFString("isLoaded")).setToNode(new SFString("TouchDown_Pose"))))
        .addChild(new HAnimHumanoid().setName(new SFString("HumanoidStub")).setInfo(new MFString18().getArray()).setVersion(new SFString("2.0"))
          .addChild(new HAnimPose().setUSE(new SFString("TouchDown_Pose")))));
    return X3D0;
    }
private class MFString0 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"Example scene for HAnimPose node"});
  }
}
private class MFColor1 {
  private org.web3d.x3d.jsail.fields.MFColor getArray() {
    return new org.web3d.x3d.jsail.fields.MFColor(new float[] {0.8f ,0.8f ,1f });
  }
}
private class MFString2 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"../Skin/JoeSkeletonSkinSite.x3d","https://www.web3d.org/x3d/content/examples/HumanoidAnimation/Skin/JoeSkeletonSkinSite.x3d"});
  }
}
private class MFString3 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"HAnimPosePrototype example implementation"});
  }
}
private class MFString4 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"SANS"});
  }
}
private class MFString5 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"MIDDLE","MIDDLE"});
  }
}
private class MFString6 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"\"T\" Pose"});
  }
}
private class MFString7 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"SANS"});
  }
}
private class MFString8 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"MIDDLE","MIDDLE"});
  }
}
private class MFString9 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"\"A\" Pose"});
  }
}
private class MFString10 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"TouchDown Pose"});
  }
}
private class MFString11 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"\"I\" Pose"});
  }
}
private class MFString12 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"Face Left Pose"});
  }
}
private class MFString13 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"Face Right Pose"});
  }
}
private class MFString14 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"Direct animation","to, from \"I\" Pose"});
  }
}
private class MFFloat15 {
  private org.web3d.x3d.jsail.fields.MFFloat getArray() {
    return new org.web3d.x3d.jsail.fields.MFFloat(new float[] {0f ,0.05f ,0.45f ,0.55f ,0.95f ,1f });
  }
}
private class MFFloat16 {
  private org.web3d.x3d.jsail.fields.MFFloat getArray() {
    return new org.web3d.x3d.jsail.fields.MFFloat(new float[] {0f ,0f ,1f ,1f ,0f ,0f });
  }
}
private class MFString17 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"Reset All Joints","to Default \"I\" Pose"});
  }
}
private class MFString18 {
  private org.web3d.x3d.jsail.fields.MFString getArray() {
    return new org.web3d.x3d.jsail.fields.MFString(new java.lang.String[] {"humanoidVersion=2.0"});
  }
}
}
