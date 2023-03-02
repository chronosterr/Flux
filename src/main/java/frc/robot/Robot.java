// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.*;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.ctre.phoenix.sensors.*;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.MecanumDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.AnalogPotentiometer;


/*            UNUSED IMPORTS
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.math.trajectory.*;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PS4Controller;
import edu.wpi.first.wpilibj.Filesystem.*;
import edu.wpi.first.wpilibj.interfaces.Gyro;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
*/

public class Robot extends TimedRobot {

  WPI_TalonSRX _driveFrontLeft = new WPI_TalonSRX(4);
  WPI_TalonSRX _driveRearLeft = new WPI_TalonSRX(2);
  WPI_TalonSRX _driveFrontRight = new WPI_TalonSRX(1);
  WPI_TalonSRX _driveRearRight = new WPI_TalonSRX(5);
  WPI_TalonSRX _intake = new WPI_TalonSRX(3);
  WPI_TalonSRX _foreArm = new WPI_TalonSRX(6);
  CANSparkMax _upperArm = new CANSparkMax(7, MotorType.kBrushless);

  AnalogPotentiometer _UAtendon = new AnalogPotentiometer(0); double zeroUAtendon = 0.0; double maxUAtendon = 1;
  AnalogPotentiometer _FAtendon = new AnalogPotentiometer(1); double zeroFAtendon = 0.967; double maxFAtendon = 0.612;
  AnalogPotentiometer _Itendon = new AnalogPotentiometer(2); double zeroItendon = 0.559; double maxItendon = 0.850;

  WPI_Pigeon2 gyro = new WPI_Pigeon2(7);

  double x, y, area, x_adjust, y_adjust, FApos, UApos, Ipos, yaw_adjust, yaw, pitch_adjust, pitch;
  float Kp, min_command, KpYaw, min_commandYaw, KpPitch, min_commandPitch;
  public boolean isForeArmZero, isUpperArmZero, 
                 isIntakeZero, isForeArmMax, 
                 isUpperArmMax, isIntakeMax,
                 isOverExtended;

  private final XboxController xbox = new XboxController(2);

  private MecanumDrive m_robotDrive;
  private Joystick l_stick = new Joystick(0);
  private Joystick r_stick = new Joystick(1);
  public Timer auto_timer = new Timer();

  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");
  
  private static final String kNDockCubeL = "No Dock Cube L of C";
  private static final String kNDockCubeR = "No Dock Cube R of C";
  private static final String kNDockMisc = "No Dock Middle";
  private static final String kNDockConeL = "No Dock Cone L of C";
  private static final String kNDockConeR = "No Dock Cone R of C";
  private static final String kYDockCubeL = "Dock Cube L of C";
  private static final String kYDockCubeC = "Dock Cube C";
  private static final String kYDockCubeR = "Dock Cube R of C";
  private static final String kYDockConeL = "Dock Cone L of C";
  private static final String kYDockConeC = "Dock Cone C";
  private static final String kYDockConeR = "Dock Cone R of C";

  private static final String kDriveAuto = "Drive Auto";
  private static final String kDefaultAuto = "Default Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  @Override
  public void robotInit() {
    Kp = -0.05f;
    KpYaw = -0.075f;
    KpPitch = -0.0235f;
    min_command = 0.05f;
    min_commandYaw = 0.075f;
    min_commandPitch = 0.02f;
    gyro.reset();
    CameraServer.startAutomaticCapture();

    _driveFrontLeft.configFactoryDefault();
    _driveRearLeft.configFactoryDefault();
    _driveFrontRight.configFactoryDefault();
    _driveRearRight.configFactoryDefault();
    _intake.configFactoryDefault();
    _foreArm.configFactoryDefault();

    _foreArm.setNeutralMode(NeutralMode.Brake); //TODO: redundant?
    
    _driveFrontLeft.setInverted(false); //redundant, but it helps my brain
    _driveRearLeft.setInverted(false); //redundant, but it helps my brain
    _driveFrontRight.setInverted(true);
    _driveRearRight.setInverted(true);
  
    m_robotDrive = new MecanumDrive(_driveFrontLeft, _driveRearLeft, _driveFrontRight, _driveRearRight);
    table.getEntry("ledMode").setNumber(1);

    m_chooser.addOption("No Dock Cube L of C", kNDockCubeL);
    m_chooser.addOption("No Dock Cube L of C", kNDockCubeR);
    m_chooser.addOption("No Dock Middle", kNDockMisc);
    m_chooser.addOption("No Dock Cone", kNDockConeL);
    m_chooser.addOption("No Dock Cone", kNDockConeR);
    m_chooser.addOption("Dock Cube L of C", kYDockCubeL);
    m_chooser.addOption("Dock Cube C", kYDockCubeC);
    m_chooser.addOption("Dock Cube R of C", kYDockCubeR);
    m_chooser.addOption("Dock Cone L of C", kYDockConeL);
    m_chooser.addOption("Dock Cone C", kYDockConeC);
    m_chooser.addOption("Dock Cone R of C", kYDockConeR);

    m_chooser.addOption("Drive Auto", kDriveAuto);

    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    SmartDashboard.putData("Auto choices", m_chooser);
  }
  
  @Override
  public void autonomousInit() {
    auto_timer.reset();
    auto_timer.start();
    gyro.reset();
    m_autoSelected = m_chooser.getSelected();
    System.out.println("Auto selected: " + m_autoSelected);
  }

  @Override
  public void autonomousPeriodic() {
    double capYaw = 0;
    checkTendons();
    x = tx.getDouble(0.0);
    y = ty.getDouble(0.0);
    switch (m_autoSelected) {
      case kNDockCubeL:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cube");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          m_robotDrive.driveCartesian(0.25, 0, 0);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, .6, 0);
          yaw_adjust = 0;
          grabGamePiece(0, "zero");
          moveForeArm(-0.75);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 12.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

          yaw = (gyro.getYaw() - 180);

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
          
          System.out.println("Yaw: " + yaw);
          System.out.println("Yaw Adjust " + yaw_adjust);
    
          m_robotDrive.driveCartesian(0, 0, -yaw_adjust);

        } else if (12.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(0.5);
          moveUpperArm(-1);
          m_robotDrive.driveCartesian(0.4, 0, 0);

        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;

      case kNDockCubeR:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cube");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          m_robotDrive.driveCartesian(0.25, 0, 0);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, -.6, 0);
          yaw_adjust = 0;
          grabGamePiece(0, "zero");
          moveForeArm(-0.75);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 12.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

          yaw = (gyro.getYaw() - 180);

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
          
          System.out.println("Yaw: " + yaw);
          System.out.println("Yaw Adjust " + yaw_adjust);
    
          m_robotDrive.driveCartesian(0, 0, -yaw_adjust);

        } else if (12.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(0.5);
          moveUpperArm(-1);
          m_robotDrive.driveCartesian(0.4, 0, 0);

        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;

      case kNDockConeL:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cone");
          
        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          m_robotDrive.driveCartesian(0.25, 0, 0);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, .6, 0);
          yaw_adjust = 0;
          grabGamePiece(0, "zero");
          moveForeArm(-0.75);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 12.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

          yaw = (gyro.getYaw() - 180);

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
          
          System.out.println("Yaw: " + yaw);
          System.out.println("Yaw Adjust " + yaw_adjust);
    
          m_robotDrive.driveCartesian(0, 0, -yaw_adjust);

        } else if (12.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(0.5);
          moveUpperArm(-1);
          m_robotDrive.driveCartesian(0.4, 0, 0);

        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;
      
      case kNDockConeR:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cone");
          
        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          m_robotDrive.driveCartesian(0.25, 0, 0);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, -.6, 0);
          yaw_adjust = 0;
          grabGamePiece(0, "zero");
          moveForeArm(-0.75);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 12.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

          yaw = (gyro.getYaw() - 180);

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
          
          System.out.println("Yaw: " + yaw);
          System.out.println("Yaw Adjust " + yaw_adjust);
    
          m_robotDrive.driveCartesian(0, 0, -yaw_adjust);

        } else if (12.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(0.5);
          moveUpperArm(-1);
          m_robotDrive.driveCartesian(0.4, 0, 0);

        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;
      
      case kNDockMisc: // drops cube atm
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cube");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          m_robotDrive.driveCartesian(0.25, 0, 0);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, -.6, 0);
          yaw_adjust = 0;
          grabGamePiece(0, "zero");
          moveForeArm(-0.75);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 15.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);
        }  else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;
        
      case kYDockConeL:
        // Implement if you aren't lazy
        break;
    
      case kYDockConeC:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cone");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          table.getEntry("pipeline").setNumber(0);
          limelightTarget(x, y);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, 0, 0);
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 10.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);
          m_robotDrive.driveCartesian(-1, 0, 0);

        } else if (10.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          chargeBalance();
        }
        break;
      
      case kYDockConeR:
        // Implement if you aren't lazy
        break;
      
      case kYDockCubeC:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(-1, "cube");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
          table.getEntry("pipeline").setNumber(0);
          limelightTarget(x, y);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          m_robotDrive.driveCartesian(-0.25, 0, 0);
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 10.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);
          if (!(gyro.getPitch() < -10)) {
            m_robotDrive.driveCartesian(-1, 0, 0);
          } else {
            m_robotDrive.driveCartesian(0, 0, 0);
          }

        } else if (10.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          chargeBalance();
        }
        break;
        
      case kYDockCubeL:
        // Implement if you aren't lazy
        break;
      
      case kDriveAuto:
        if (0.0 < auto_timer.get() && auto_timer.get() < 2.0) {
          m_robotDrive.driveCartesian(0.5, 0, 0);
        } else if (2.0 < auto_timer.get() && auto_timer.get() < 5.0) {
          m_robotDrive.driveCartesian(0, 0, -.5);
        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
        }
        break;
      case kDefaultAuto:
      default:
        // Put default auto code here
        break;
    }
  }

  public void checkTendons() {
    FApos = _FAtendon.get();
    UApos = _UAtendon.get();
    Ipos = _Itendon.get();

    isForeArmMax = false;
    isForeArmZero = false;
    isUpperArmMax = false;
    isUpperArmZero = false;
    isIntakeMax = false;
    isIntakeZero = false;

    if (FApos > zeroFAtendon) { //checks if potentiometer in zero range (+ or - 0.05)
      isForeArmZero = true;
    } else if (FApos < maxFAtendon) {
      isForeArmMax = true;
    }
    
    if (UApos < zeroUAtendon) { //checks if potentiometer is in zero range (+ or - 0.05)
      isUpperArmZero = true;
    } else if (UApos > maxUAtendon) {
      isUpperArmMax = true;
    }

    if (Ipos < zeroItendon) { //checks if potentiometer is in zero range (+ or - 0.05)
      isIntakeZero = true;
    } else if (Ipos > maxItendon) {
      isIntakeMax = true;
    }

    SmartDashboard.putBoolean("isUpperArmZero", isUpperArmZero);
    SmartDashboard.putBoolean("isUpperArmMax", isUpperArmMax);
    SmartDashboard.putBoolean("isForeArmZero", isForeArmZero);
    SmartDashboard.putBoolean("isForeArmMax", isForeArmMax);
    SmartDashboard.putBoolean("isIntakeZero", isIntakeZero);
    SmartDashboard.putBoolean("isIntakeMax", isIntakeMax);
  }

  public void checkOverExtended() {
    // The goal is to get FA to look like UA. I hypothesize we can do this by flipping the sign of FA and adding two. Testing required.
    double UA = _UAtendon.get();
    double FA = -_FAtendon.get() + 2;
      if(Math.abs(FA - UA) < 0.5) { // change 0.5 based on testing. that number resembles the allowed VARIANCE between FA and UA.
        // Forearm Potentiometer limit with UA zero: 0.695
        isOverExtended = true;
      } else {
        isOverExtended = false;
      }
    
    SmartDashboard.putBoolean("isOverExtended", isOverExtended);
  }

  public void chargeBalance() {
    _driveFrontLeft.setNeutralMode(NeutralMode.Coast);
    _driveFrontRight.setNeutralMode(NeutralMode.Coast);
    _driveRearLeft.setNeutralMode(NeutralMode.Coast);
    _driveRearRight.setNeutralMode(NeutralMode.Coast);

    yaw = gyro.getYaw() % 360;
    pitch = gyro.getPitch();
    yaw_adjust = 0.0f;

    if (Math.abs(pitch) > min_commandPitch) {
      pitch_adjust = KpPitch * pitch; // desired max ~.3
    }

    if (Math.abs(yaw) > min_commandYaw) {
      yaw_adjust = KpYaw * yaw; // desired max ???
    }

    
    if (yaw_adjust == 0 && pitch_adjust == 0) {
      m_robotDrive.driveCartesian(0, 0, 0);
      _driveFrontLeft.setNeutralMode(NeutralMode.Brake);
      _driveFrontRight.setNeutralMode(NeutralMode.Brake);
      _driveRearLeft.setNeutralMode(NeutralMode.Brake);
      _driveRearRight.setNeutralMode(NeutralMode.Brake);
    } else {
      m_robotDrive.driveCartesian(-pitch_adjust, 0, -yaw_adjust);
    }
  }

  public void limelightTarget(double x, double y) { //should import x and y limelight values if used
    table.getEntry("ledMode").setNumber(3); //turns on LEDs
    this.x = x;
    this.y = y;
    x_adjust = 0.0f;
    y_adjust = 0.0f;

    if (Math.abs(x) > min_command) {
      x_adjust = Kp * x;
    } else if (Math.abs(x) < min_command) {
      x_adjust = 0;
    }

    if (Math.abs(y) > min_command) {
      y_adjust = Kp * y;
    } else if (Math.abs(y) < min_command) {
      y_adjust = 0;
    }
    m_robotDrive.driveCartesian(y_adjust - l_stick.getY(), -x_adjust + l_stick.getX(), 0.0 + r_stick.getX());
  }

  public void aprilTagsTarget(double x, double y) { //should import x and y limelight values if used
    table.getEntry("pipeline").setNumber(1);
    table.getEntry("ledMode").setNumber(3); //turns on LEDs
    this.x = x;
    this.y = y;
    x_adjust = 0.0f;
    y_adjust = 0.0f;
    if (Math.abs(x) > min_command) {
      x_adjust = Kp * x;
    } else if (Math.abs(x) < min_command) {
      x_adjust = 0;
    } if (Math.abs(y) > min_command) {
      y_adjust = Kp * y;
    } else if (Math.abs(y) < min_command) {
      y_adjust = 0;
    }
    m_robotDrive.driveCartesian(y_adjust - l_stick.getY(), 0.0 + l_stick.getX(), -x_adjust + r_stick.getX());
  }

  public void zeroedPosition() {
    if (!isUpperArmZero) {
      if (_upperArm.get() - 0.05 > zeroUAtendon) {
        _upperArm.set(-0.2);
      } else if (_upperArm.get() + 0.05 < zeroUAtendon) {
        _upperArm.set(0.2);
      }
    }

    if (!isForeArmZero) {
      if (_FAtendon.get() - 0.05 > zeroFAtendon) {
        _foreArm.set(-0.2);
      } else if (_FAtendon.get() + 0.05 < zeroFAtendon) {
        _foreArm.set(0.2);
      }
    }
  }

  public void moveUpperArm(double speed) {
    if (speed > 0) {
      if (isUpperArmZero) {
        _upperArm.set(0);
      } else {
        _upperArm.set(speed);
      }
    } else if (speed < 0) {
      if (isUpperArmMax) {
        _upperArm.set(0);
      } else {
        _upperArm.set(speed);
      }
    } else {
      _upperArm.set(0);
    }
  }

  // public void moveUpperArm(double speed) {
  //   if (speed < 0) {
  //     _upperArm.set(-0.75);
  //   } else if (speed > 0) {
  //     _upperArm.set(0.75);
  //   } else {
  //     _upperArm.set(0);
  //   }
  // }

  public void moveForeArm(double speed) {
    if (speed < 0) {
      if (isForeArmZero) {
        _foreArm.set(0);
      } else {
        _foreArm.set(speed);
      }
    } else if (speed > 0) {
      if (isForeArmMax) {
        _foreArm.set(0);
      } else {
        _foreArm.set(speed);
      }
    } else {
      _foreArm.set(0);
    }
  }

  public void grabGamePiece(double speed, String piece) {
    double zeroCube = 0.745;
    double zeroCone = zeroItendon;
    switch (piece) {
      case "cube":
        if (speed < 0 && _Itendon.get() > zeroCube) {
          _intake.set(speed);
        } else {
          _intake.set(0);
        }
        break;
      case "cone":
        if (speed < 0 && _Itendon.get() > zeroCone) {
           _intake.set(speed);
        } else {
          _intake.set(0);
        }
        break;
      case "zero":
        if (!isIntakeZero) {
          _intake.set(speed);
        } else {
          _intake.set(0);
        }
        break;
      case "open":
        if (!isIntakeMax) {
          _intake.set(speed);
        } else {
          _intake.set(0);
        }
        break;
      default:
        table.getEntry("ledMode").setNumber(2);
        System.out.println("Error @ grabGamePiece - Neither cube nor cone selected!");
        break;
      }
  }

  @Override
  public void teleopPeriodic() {
    // Use the joystick X axis for lateral movement, Y axis for forward
    // movement, and Z axis for rotation.
    //XBOX DRIVE IS BELOW
    // m_robotDrive.driveCartesian(-xbox.getLeftY(), -xbox.getLeftX(), xbox.getRightX(), 0.0);
    
    //FIELD-ORIENTED DRIVE
    //m_robotDrive.driveCartesian(-l_stick.getY(), l_stick.getX(), r_stick.getZ(), gyro.getAngle()+180);

    //ROBOT-ORIENTED DRIVE
    //double intensity = (l_stick.getThrottle() + 1) / 2; (CORRECT)

    m_robotDrive.driveCartesian(-l_stick.getY(), l_stick.getX(), r_stick.getZ());

    int L1 = 5;
    int R1 = 6;
    int A = 1;
    int B = 2;
    int X = 3;
    int Y = 4;
    int L3 = 9;
    int R3 = 10;

    checkTendons();
    checkOverExtended();
    SmartDashboard.putNumber("Upper Arm Potentiometer value", _UAtendon.get());
    SmartDashboard.putNumber("Forearm Potentiometer value", _FAtendon.get());
    SmartDashboard.putNumber("Intake Potentiometer value", _Itendon.get());

    SmartDashboard.putNumber("Yaw", gyro.getYaw());
    SmartDashboard.putNumber("Pitch", gyro.getPitch());
    SmartDashboard.putNumber("Roll", gyro.getRoll());

    //UPPER ARM CONTROL
    if (xbox.getLeftY() > .2) {
      moveUpperArm(1);
    } else if (xbox.getLeftY() < -.2) {
      moveUpperArm(-1);
    } else {
      moveUpperArm(0);
    }

    //FOREARM CONTROL
    if (xbox.getRightY() > .2) {
      moveForeArm(-1);
    } else if (xbox.getRightY() < -.2) {
      moveForeArm(1);
    } else {
      moveForeArm(0);
    }

    // INTAKE CONTROL
    if (xbox.getRawButton(Y)) { // Opens with Y
      grabGamePiece(1, "open");
    } else if (xbox.getRawButton(X)) { // Grabs cube with X
      grabGamePiece(-1, "cube");
    } else if (xbox.getRawButton(B)) { // Grabs cone with B
      grabGamePiece(-1, "cone");
    } else if (xbox.getRawButton(A)) { // Closes fully with A
      grabGamePiece(-1, "zero");
    } else { //Don't move!
      grabGamePiece(0, "zero");
    }

    x = tx.getDouble(0.0);
    y = ty.getDouble(0.0);
    if (l_stick.getRawButton(1)) { // LIMELIGHT uncovered targeting
      table.getEntry("pipeline").setNumber(0);
      limelightTarget(x, y);
    } else if (l_stick.getRawButton(5)) { // LIMELIGHT top covered?
      table.getEntry("pipeline").setNumber(3); 
      limelightTarget(x, y);
    } else if (l_stick.getRawButton(3)) { // LIMELIGHT bottom covered?
      table.getEntry("pipeline").setNumber(2);
      limelightTarget(x, y);
    } else if (r_stick.getRawButton(1)) { //APRILTAG targeting
      table.getEntry("pipeline").setNumber(1);
      aprilTagsTarget(x, y);
    } else if (l_stick.getRawButton(2)) {
      table.getEntry("ledMode").setNumber(3); //turns on LEDs
    } else {
      table.getEntry("ledMode").setNumber(1); //turns off LEDs
    }

    if (r_stick.getRawButton(2)) {
      if (r_stick.getRawButtonPressed(2)) {
        gyro.reset();
      }
      chargeBalance();
    }

    if (l_stick.getRawButton(11)) {
      yaw = gyro.getYaw() % 360;
      if (Math.abs(yaw + 180) > min_commandYaw) {
        yaw_adjust = KpYaw * yaw;
      }

      if (Math.abs(yaw_adjust) > 0.5) {
        if (yaw_adjust < 0) {
          yaw_adjust = -0.5;
        } else {
          yaw_adjust = 0.5;
        }
      }
      
      System.out.println("Yaw: " + yaw);
      System.out.println("Yaw Adjust " + yaw_adjust);

      m_robotDrive.driveCartesian(0, 0, -yaw_adjust);
    }
  }
}