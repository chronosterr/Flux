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
import edu.wpi.first.math.filter.SlewRateLimiter;
*/

public class Robot extends TimedRobot {
// TODO: Optimize. (moveUpperArm, for example)
  WPI_TalonSRX _driveFrontLeft = new WPI_TalonSRX(4);
  WPI_TalonSRX _driveRearLeft = new WPI_TalonSRX(2);
  WPI_TalonSRX _driveFrontRight = new WPI_TalonSRX(1);
  WPI_TalonSRX _driveRearRight = new WPI_TalonSRX(5);
  WPI_TalonSRX _intake = new WPI_TalonSRX(3);
  WPI_TalonSRX _foreArm = new WPI_TalonSRX(6);
  CANSparkMax _upperArm = new CANSparkMax(7, MotorType.kBrushless);

  AnalogPotentiometer _UAtendon = new AnalogPotentiometer(0); double zeroUAtendon = 0.531; double maxUAtendon = 0.652; // 0.535 0.643 | 0.526 0.631 | 0.500 0.600 | 0.520 0.620
  AnalogPotentiometer _FAtendon = new AnalogPotentiometer(1); double zeroFAtendon = 0.967; double maxFAtendon = 0.612;
  AnalogPotentiometer _Itendon = new AnalogPotentiometer(2); double zeroItendon = 0.559; double maxItendon = 0.850;

  WPI_Pigeon2 gyro = new WPI_Pigeon2(7);

  double x, y, area, x_adjust, y_adjust, FApos, UApos, Ipos, yaw_adjust, yaw, pitch_adjust, pitch;
  float Kp, min_command, KpYaw, min_commandYaw, KpPitch, min_commandPitch;
  public boolean isForeArmZero, isUpperArmZero, 
                 isIntakeZero, isForeArmMax, 
                 isUpperArmMax, isIntakeMax,
                 isOverExtended,
                 isChargeTipped, isChargeLevel;

  private final XboxController xbox = new XboxController(2);

  private MecanumDrive m_robotDrive;
  private Joystick l_stick = new Joystick(0);
  private Joystick r_stick = new Joystick(1);

  public Timer auto_timer = new Timer();

  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");
  
  private static final String kNCare = "No, We Don't Care";
  private static final String kYCare = "Yes, We Care";

  private static final String kNDockCubeL = "No Dock Cube L of C";
  private static final String kNDockCubeR = "No Dock Cube R of C";
  private static final String kNDockCubeMisc = "No Dock Cube Straight";
  private static final String kNDockConeMisc = "No Dock Cone Straight";
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
  private String m_OESelected;
  public boolean OECheck = false;
  private final SendableChooser<String> m_autoChooser = new SendableChooser<>();
  private final SendableChooser<String> m_overExtensionChooser = new SendableChooser<>();

  @Override
  public void robotInit() {
    Kp = -0.035f;
    KpYaw = -0.075f;
    KpPitch = (float)SmartDashboard.getNumber("chargeBalance Power", -0.025f);

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

    _foreArm.setNeutralMode(NeutralMode.Brake); // possibly redundant, but it helps my brain
    
    _driveFrontLeft.setInverted(false); // redundant, but it helps my brain
    _driveRearLeft.setInverted(false); // redundant, but it helps my brain
    _driveFrontRight.setInverted(true);
    _driveRearRight.setInverted(true);
  
    m_robotDrive = new MecanumDrive(_driveFrontLeft, _driveRearLeft, _driveFrontRight, _driveRearRight);
    table.getEntry("ledMode").setNumber(1);

    m_overExtensionChooser.addOption("Yes, We Care", kYCare);
    m_overExtensionChooser.addOption("No, We Don't Care", kNCare);
    m_overExtensionChooser.setDefaultOption("Default (No)", kNCare);
    SmartDashboard.putData("Anti-Overextension Code?", m_overExtensionChooser);
    SmartDashboard.putNumber("chargeBalance Power", -0.025f);

    m_autoChooser.addOption("No Dock Cube L of C", kNDockCubeL);
    m_autoChooser.addOption("No Dock Cube R of C", kNDockCubeR);
    m_autoChooser.addOption("No Dock Cube Straight", kNDockCubeMisc);
    m_autoChooser.addOption("No Dock Cone Straight", kNDockConeMisc);
    m_autoChooser.addOption("No Dock Cone L of C", kNDockConeL);
    m_autoChooser.addOption("No Dock Cone R of C", kNDockConeR);
    m_autoChooser.addOption("Dock Cube L of C", kYDockCubeL);
    m_autoChooser.addOption("Dock Cube C", kYDockCubeC);
    m_autoChooser.addOption("Dock Cube R of C", kYDockCubeR);
    m_autoChooser.addOption("Dock Cone L of C", kYDockConeL);
    m_autoChooser.addOption("Dock Cone C", kYDockConeC);
    m_autoChooser.addOption("Dock Cone R of C", kYDockConeR);

    m_autoChooser.addOption("Drive Auto", kDriveAuto);

    m_autoChooser.setDefaultOption("Default Auto", kDefaultAuto);
    SmartDashboard.putData("Auto choices", m_autoChooser);
  }
  
  @Override
  public void autonomousInit() {
    auto_timer.reset();
    auto_timer.start();
    gyro.reset();
    m_autoSelected = m_autoChooser.getSelected();
    System.out.println("Auto selected: " + m_autoSelected);

    KpPitch = (float)SmartDashboard.getNumber("chargeBalance Power", -0.025f);
    isChargeTipped = false;
    isChargeLevel = false;
  }

  @Override
  public void autonomousPeriodic() {
    // TODO: Cone auto doesn't function with the type of reflective tape used. Find new reflective tape and add 2 more cone autos depending on whether you're on the close or far side (two sets of pipes next to each other!!)
    // TODO: Dock auto is not currently possible. (Possibly) remake charge station and find a way to stay still with mecanum wheels!

    checkTendons();
    x = tx.getDouble(0.0);
    y = ty.getDouble(0.0);
    switch (m_autoSelected) {
      case kNDockCubeL: case kNDockCubeR: // takes both cases (only difference is strafe, which is dictated when necessary)
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(1, "cube");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) { // TODO: Forearm goes too high, leading to bouncing. Add second parameter to foreArm to set (percentage extended or direct value) of how high to go.
          m_robotDrive.driveCartesian(0.25, 0, 0);
          moveForeArm(1);
          moveUpperArm(-0.75);

        } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
          m_robotDrive.driveCartesian(0, 0, 0);
          grabGamePiece(1, "open");
          moveForeArm(0);
          moveUpperArm(0);

        } else if (7.5 < auto_timer.get() && auto_timer.get() < 8.5) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
    
          if (m_autoSelected == kNDockCubeL) {
            m_robotDrive.driveCartesian(-0.25, .5, -yaw_adjust);
          } else {
            m_robotDrive.driveCartesian(-0.25, -.5, -yaw_adjust);
          }

          yaw_adjust = 0;

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 12.0) {
          moveForeArm(-1);
          moveUpperArm(1);
          grabGamePiece(0, "zero");

          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }

          m_robotDrive.driveCartesian(-0.27, 0, -yaw_adjust);
          yaw_adjust = 0;
      
        } else if (12.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(0);
          moveUpperArm(0);

          yaw = (gyro.getYaw() + 180);

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
    
          m_robotDrive.driveCartesian(0, 0, -yaw_adjust);

        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;
      
      case kNDockConeL: case kNDockConeR:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(1, "cone");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 7.0) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-1);

        } else if (7.0 < auto_timer.get() && auto_timer.get() < 9.0) {
          limelightTarget(x, y);
          moveForeArm(0);
          moveUpperArm(0);

        } else if (9.0 < auto_timer.get() && auto_timer.get() < 9.5) {
          limelightTarget(x, y);
          moveForeArm(-1);

        } else if (9.5 < auto_timer.get() && auto_timer.get() < 10.0) {
          limelightTarget(x, y);
          grabGamePiece(0.5, "open");
          moveForeArm(-0.8);

        } else if (10.0 < auto_timer.get() && auto_timer.get() < 11.0) {
          grabGamePiece(1, "open");
          if (10.0 < auto_timer.get() && auto_timer.get() < 10.5) {
            moveForeArm(1);
          } else {
            moveForeArm(-1);
          }
          moveUpperArm(1);
          table.getEntry("ledMode").setNumber(1); // turns off limelight LED

          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
          
          if (m_autoSelected == kNDockConeL) {
            m_robotDrive.driveCartesian(-0.25, .75, -yaw_adjust);
          } else {
            m_robotDrive.driveCartesian(-0.25, -.75, -yaw_adjust);
          }
          yaw_adjust = 0;

        } else if (11.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(-1);
          moveUpperArm(1);
          grabGamePiece(0, "zero");

          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }

          m_robotDrive.driveCartesian(-0.25, 0, -yaw_adjust);
          yaw_adjust = 0;
      
        } else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;
      
      case kNDockCubeMisc:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(1, "cube");

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
          m_robotDrive.driveCartesian(-0.25, 0, 0);
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);

        } else if (8.5 < auto_timer.get() && auto_timer.get() < 15.0) {
          grabGamePiece(0, "zero");
          moveForeArm(-1);
          moveUpperArm(1);
          
          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }

          m_robotDrive.driveCartesian(0, 0, -yaw_adjust);
          yaw_adjust = 0;

        }  else {
          m_robotDrive.driveCartesian(0, 0, 0);
          moveUpperArm(0);
          moveForeArm(0);
          grabGamePiece(0, "zero");
        }
        break;
      
      case kNDockConeMisc:
        if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
          grabGamePiece(1, "cone");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 7.0) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-1);

        } else if (7.0 < auto_timer.get() && auto_timer.get() < 9.0) {
          limelightTarget(x, y);
          moveForeArm(0);
          moveUpperArm(0);

        } else if (9.0 < auto_timer.get() && auto_timer.get() < 9.5) {
          limelightTarget(x, y);
          moveForeArm(-1);

        } else if (9.5 < auto_timer.get() && auto_timer.get() < 10.0) {
          limelightTarget(x, y);
          grabGamePiece(0.5, "open");
          moveForeArm(-0.8);

        } else if (10.0 < auto_timer.get() && auto_timer.get() < 11.0) {
          grabGamePiece(1, "open");
          if (10.0 < auto_timer.get() && auto_timer.get() < 10.5) {
            moveForeArm(1);
          } else {
            moveForeArm(-1);
          }
          moveUpperArm(1);
          table.getEntry("ledMode").setNumber(1); // turns off limelight LED

          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }
          
          m_robotDrive.driveCartesian(-0.25, 0, -yaw_adjust);
          yaw_adjust = 0;

        } else if (11.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          moveForeArm(-1);
          moveUpperArm(1);
          grabGamePiece(0, "zero");

          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }
    
          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }

          m_robotDrive.driveCartesian(-0.25, 0, -yaw_adjust);
          yaw_adjust = 0;
      
        } else {
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
          grabGamePiece(1, "cone");

        } else if (1.0 < auto_timer.get() && auto_timer.get() < 7.0) {
          grabGamePiece(0, "zero");
          moveForeArm(1);
          moveUpperArm(-1);

        } else if (7.0 < auto_timer.get() && auto_timer.get() < 9.0) {
          limelightTarget(x, y);
          moveForeArm(0);
          moveUpperArm(0);

        } else if (9.0 < auto_timer.get() && auto_timer.get() < 9.5) {
          limelightTarget(x, y);
          moveForeArm(-1);

        } else if (9.5 < auto_timer.get() && auto_timer.get() < 10.0) {
          limelightTarget(x, y);
          grabGamePiece(0.5, "open");
          moveForeArm(-0.8);

        } else if (10.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          grabGamePiece(0, "zero");
          if (10.0 < auto_timer.get() && auto_timer.get() < 10.5) {
            moveForeArm(1);
          } else {
            moveForeArm(-0.4);
          }
          moveUpperArm(0.5);
          table.getEntry("ledMode").setNumber(1); // turns off limelight LED

          if (gyro.getPitch() < -10) {
            isChargeTipped = true;
          } if (gyro.getPitch() > 1) {
            isChargeLevel = true;
          }
          
          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }

          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }

          if (!isChargeTipped && !isChargeLevel) m_robotDrive.driveCartesian(-1, 0, -yaw_adjust);
          // if (isChargeTipped && !isChargeLevel) m_robotDrive.driveCartesian(-0.35, 0, -yaw_adjust);
          if (isChargeTipped) chargeBalance();
        } else {
          grabGamePiece(0, "zero");
          moveForeArm(0);
          moveUpperArm(0);
          m_robotDrive.driveCartesian(0, 0, 0);

        }
        break;
      
      case kYDockConeR:
        // Implement if you aren't lazy
        break;
      
      case kYDockCubeC:
        // if (0.0 < auto_timer.get() && auto_timer.get() < 1.0) {
        //   grabGamePiece(1, "cube");

        // } else if (1.0 < auto_timer.get() && auto_timer.get() < 4.5) {
        //   grabGamePiece(0, "zero");
        //   moveForeArm(1);
        //   moveUpperArm(-0.75);

        // } else if (4.5 < auto_timer.get() && auto_timer.get() < 6.0) {
        //   m_robotDrive.driveCartesian(0.25, 0, 0);
        //   moveForeArm(1);
        //   moveUpperArm(-0.75);

        // } else if (6.0 < auto_timer.get() && auto_timer.get() < 7.5) {
        //   m_robotDrive.driveCartesian(0, 0, 0);
        //   grabGamePiece(1, "open");
        //   moveForeArm(0);
        //   moveUpperArm(0);

        // } else if (7.5 < auto_timer.get() && auto_timer.get() < 10.0) {
        //   m_robotDrive.driveCartesian(-0.25, 0, 0);
        //   grabGamePiece(0, "zero");
        //   moveForeArm(-1);
        //   moveUpperArm(0.75);

        // } else 
        if (5.0 < auto_timer.get() && auto_timer.get() < 15.0) {
          SmartDashboard.putBoolean("isChargeTipped", isChargeTipped);
          SmartDashboard.putBoolean("isChargeLevel", isChargeLevel);

          grabGamePiece(0, "zero");
          moveForeArm(-0.4);
          moveUpperArm(0.50);
          if (gyro.getPitch() < -10) {
            isChargeTipped = true;
          } if (gyro.getPitch() > 3) {
            isChargeLevel = true;
          }
          
          yaw = gyro.getYaw();

          if (Math.abs(yaw) > min_commandYaw) {
            yaw_adjust = KpYaw * yaw;
          }

          if (yaw_adjust > 0.5) {
            yaw_adjust = 0.5;
          } else if (yaw_adjust < -0.5) {
            yaw_adjust = -0.5;
          }

          if (!isChargeTipped && !isChargeLevel) m_robotDrive.driveCartesian(-1, 0, -yaw_adjust);
          // if (isChargeTipped && !isChargeLevel) m_robotDrive.driveCartesian(-0.3, 0, -yaw_adjust);
          if (isChargeTipped) chargeBalance();
        } else {
          grabGamePiece(0, "zero");
          moveForeArm(0);
          moveUpperArm(0);
          m_robotDrive.driveCartesian(0, 0, 0);

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

    if (FApos > zeroFAtendon) { // checks if FA has exceeded zero
      isForeArmZero = true;
    } else if (FApos < maxFAtendon) { // checks if FA has exceeded max
      isForeArmMax = true;
    }
    
    if (UApos < zeroUAtendon) { // checks if UA has exceeded zero
      isUpperArmZero = true;
    } else if (UApos > maxUAtendon) { // checks if UA has exceeded max
      isUpperArmMax = true;
    }

    if (Ipos < zeroItendon) { // checks if intake has exceeded 0
      isIntakeZero = true;
    } else if (Ipos > maxItendon) { // checks if intake has exceeded max
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
    isOverExtended = false;
    double UA = _UAtendon.get();
    double FA = _FAtendon.get();

    if (UA <= 0.596) { // 48" - 46"
      if (FA < 0.682) {isOverExtended = true;}
    } else if (0.596 < UA && UA <= 0.609) { // 46" - 44"
      if (FA < 0.631) {isOverExtended = true;}
    } else if (0.609 < UA && UA <= 0.618) { // 44" - 42"
      if (FA < 0.618) {isOverExtended = true;}
    }

    // between 42" and 38", FA can be any value.
    
    SmartDashboard.putBoolean("isOverExtended", isOverExtended);
  }

  public void chargeBalance() {
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
      _driveFrontLeft.set(-0.25);
      _driveFrontRight.set(-0.25);
      _driveRearLeft.set(0.25);
      _driveRearRight.set(0.25);

      _driveFrontLeft.setNeutralMode(NeutralMode.Brake);
      _driveFrontRight.setNeutralMode(NeutralMode.Brake);
      _driveRearLeft.setNeutralMode(NeutralMode.Brake);
      _driveRearRight.setNeutralMode(NeutralMode.Brake);
    } else {
      m_robotDrive.driveCartesian(-pitch_adjust - l_stick.getY(), 0.0 + l_stick.getX(), -yaw_adjust + r_stick.getZ());
    }
  }

  /*
   * LIMELIGHT CHEAT SHEET!
   * ledMode:
   * 0 - Pipeline default
   * 1 - Off
   * 2 - Flash
   * 3 - On
   * 
   * pipeline:
   * 0 - Reflective Tape UNCOVERED
   * 1 - AprilTags (UNUSED)
   * 2 - Reflective Tape BOTTOM COVERED
   * 3 - Reflective Tape TOP COVERED
   */

  public void limelightTarget(double x, double y) {
    table.getEntry("ledMode").setNumber(3);
    this.x = x;
    this.y = y;
    x_adjust = 0.0f;
    y_adjust = 0.0f;
    yaw_adjust = 0;

    if (Math.abs(x) > 0.13f) {
      x_adjust = -0.13f * x;
    } else if (Math.abs(x) < 0.13f) {
      x_adjust = 0;
    }

    if (Math.abs(y) > min_command) {
      y_adjust = Kp * y;
    } else if (Math.abs(y) < min_command) {
      y_adjust = 0;
    }

    yaw = gyro.getYaw();

    if (Math.abs(yaw) > min_commandYaw) {
      yaw_adjust = KpYaw * yaw;
    }

    if (yaw_adjust > 0.5) {
      yaw_adjust = 0.5;
    } else if (yaw_adjust < -0.5) {
      yaw_adjust = -0.5;
    }

    SmartDashboard.putNumber("x_adjust", x_adjust);
    SmartDashboard.putNumber("y_adjust", y_adjust);
    SmartDashboard.putNumber("yaw_adjust", yaw_adjust);
    m_robotDrive.driveCartesian(y_adjust - l_stick.getY(), -x_adjust + l_stick.getX(), -yaw_adjust + r_stick.getX());
  }

  public void aprilTagsTarget(double x, double y) {
    table.getEntry("pipeline").setNumber(1);
    table.getEntry("ledMode").setNumber(3);
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

  public void moveUpperArm(double speed) { // RELY ON THAT CLUTCH BABY
    if (speed > 0) {  
      if (isOverExtended && OECheck) {
          _upperArm.set(0);
        } else {
        _upperArm.set(speed);
        }

      } else if (speed < 0) {
        if (isUpperArmMax && OECheck) {
          _upperArm.set(0);
        } else {
          _upperArm.set(speed);
        }
      } else {
        _upperArm.set(0);
      }
    }

  public void moveForeArm(double speed) {
    if (speed < 0) {
      if (isForeArmZero) {
        _foreArm.set(0);
      } else {
        _foreArm.set(speed);
      }
    } else if (speed > 0) {
      if (isForeArmMax || (isOverExtended && OECheck)) {
        _foreArm.set(0);
      } else {
        _foreArm.set(speed);
      }
    } else {
      _foreArm.set(0);
    }
  }
  public void moveUpperArmToPos(double speed, double desiredPos) { // speed should always be positive!
    double UApos = _UAtendon.get();
    if (desiredPos - UApos > 0) { // move UP
      if (isUpperArmMax && OECheck) {
        _upperArm.set(0);
      } else {
        _upperArm.set(-speed);
      }
    } else if (desiredPos - UApos > 0) { // move DOWN
      if (isOverExtended && OECheck) {
        _upperArm.set(0);
      } else {
        _upperArm.set(speed);
      }
    } else {
      _upperArm.set(0);
    }
  }

  public void moveForeArmToPos(double speed, double desiredPos) { // speed should always be positive!
    double FApos = _FAtendon.get();
    if (desiredPos - FApos < 0) { // move UP
      if (isForeArmMax || (isOverExtended && OECheck)) {
        _foreArm.set(0);
      } else {
        _foreArm.set(speed);
      }
    } else if (desiredPos - FApos > 0) { // move DOWN
      if (isForeArmZero) {
        _foreArm.set(0);
      } else {
        _foreArm.set(-speed);
      }
    } else {
      _foreArm.set(0);
    }
  }

  public void moveToIntakeFloor(double speed) {
    double desiredUA = 0.618;
    double desiredFA = 0.909;
    double currUA = _UAtendon.get();
    double currFA = _FAtendon.get();

    if (currFA - 0.05 > desiredFA && currFA + 0.05 < desiredFA) { // sets a variance of 0.05 (Requires testing)
      if (currUA - 0.05 > desiredUA && currUA + 0.05 < desiredUA) {
        moveForeArm(0);
        moveUpperArm(0);
      } else {
        moveUpperArmToPos(1, desiredUA);
        moveForeArm(0);
      }
    } else {
      moveForeArmToPos(1, desiredFA);
      moveUpperArm(0);
    }
  }

  public void moveToIntakeHP(double speed) {
    double desiredUA = 0.537;
    double desiredFA = 0.778;
    double currUA = _UAtendon.get();
    double currFA = _FAtendon.get();

    if (currFA - 0.05 > desiredFA && currFA + 0.05 < desiredFA) { // sets a variance of 0.05 (Requires testing)
      if (currUA - 0.05 > desiredUA && currUA + 0.05 < desiredUA) {
        moveForeArm(0);
        moveUpperArm(0);
      } else {
        moveUpperArmToPos(1, desiredUA);
        moveForeArm(0);
      }
    } else {
      moveForeArmToPos(1, desiredFA);
      moveUpperArm(1); // Necessary? Safe? Faster?
    }
  }

  public void grabGamePiece(double speed, String piece) {
    double zeroCube = 0.745;
    double zeroCone = zeroItendon;
    switch (piece) {
      case "cube":
        if (_Itendon.get() > zeroCube && !isIntakeZero) {
          _intake.set(-speed);
        } else {
          _intake.set(0);
        }
        break;
      case "cone":
        if (_Itendon.get() > zeroCone && !isIntakeZero) {
           _intake.set(-speed);
        } else {
          _intake.set(0);
        }
        break;
      case "zero": // closed
        if (!isIntakeZero) {
          _intake.set(-speed);
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
  public void teleopInit() {
    m_OESelected = m_overExtensionChooser.getSelected();
    if (m_OESelected == kYCare) {
      OECheck = true;
    } else {
      OECheck = false;
    }

    KpPitch = (float)SmartDashboard.getNumber("chargeBalance Power", -0.025f);
  }

  @Override
  public void teleopPeriodic() {
    // Use the L joystick X axis for lateral movement, Y axis for forward
    // movement, and R joystick Z axis for rotation.
    // XBOX DRIVE IS BELOW
    // m_robotDrive.driveCartesian(-xbox.getLeftY(), -xbox.getLeftX(), xbox.getRightX(), 0.0);
    
    // FIELD-ORIENTED DRIVE
    // m_robotDrive.driveCartesian(-l_stick.getY(), l_stick.getX(), r_stick.getZ(), gyro.getRotation2d());

    // SLEW-FILTERED DRIVE (for top-heavy or powerful drive bots. Too slow on Flux)
    // m_robotDrive.driveCartesian(l_filter.calculate(-l_stick.getY()), l_stick.getX(), r_filter.calculate(r_stick.getZ()));

    // ROBOT-ORIENTED DRIVE
    m_robotDrive.driveCartesian(-l_stick.getY(), l_stick.getX(), r_stick.getZ());

    int A = 1;
    int B = 2;
    int X = 3;
    int Y = 4;
    // int L1 = 5;
    // int R1 = 6;
    // int L3 = 9;
    // int R3 = 10;

    checkTendons();
    checkOverExtended();
    SmartDashboard.putNumber("Upper Arm Potentiometer value", _UAtendon.get());
    SmartDashboard.putNumber("Forearm Potentiometer value", _FAtendon.get());
    SmartDashboard.putNumber("Intake Potentiometer value", _Itendon.get());

    SmartDashboard.putNumber("Yaw", gyro.getYaw());
    SmartDashboard.putNumber("Pitch", gyro.getPitch());
    SmartDashboard.putNumber("Roll", gyro.getRoll());

    // UPPER ARM CONTROL
    if (xbox.getLeftY() > .2) {
      moveUpperArm(1);
    } else if (xbox.getLeftY() < -.2) {
      moveUpperArm(-1);
    } else {
      moveUpperArm(0);
    }

    // FOREARM CONTROL
    if (xbox.getRightY() > .2) {
      moveForeArm(-1);
    } else if (xbox.getRightY() < -.2) {
      moveForeArm(1);
    } else {
      moveForeArm(0);
    }

    // MOVE ARMS TO INTAKE FROM FLOOR
    if (xbox.getLeftTriggerAxis() > 0.50) {
      moveToIntakeFloor(1);
    }

    // MOVE ARMS TO INTAKE FROM HUMAN PLAYER
    if (xbox.getRightTriggerAxis() > 0.50) {
      moveToIntakeHP(1);
    }

    // INTAKE CONTROL
    if (xbox.getRawButton(Y)) { // Opens with Y
      grabGamePiece(1, "open");
    } else if (xbox.getRawButton(X)) { // Grabs cube with X
      grabGamePiece(1, "cube");
    } else if (xbox.getRawButton(B)) { // Grabs cone with B
      grabGamePiece(1, "cone");
    } else if (xbox.getRawButton(A)) { // Closes fully with A
      grabGamePiece(1, "zero");
    } else { // Don't move!
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
    } else if (r_stick.getRawButtonPressed(1)) { // snapshot taker (requires pit testing)
      table.getEntry("snapshot").setNumber(1);
    } else if (l_stick.getRawButton(2)) {
      table.getEntry("ledMode").setNumber(3);
    } else {
      table.getEntry("ledMode").setNumber(1);
      table.getEntry("snapshot").setNumber(0);
    }
    
    if (r_stick.getRawButton(2)) {
      if (r_stick.getRawButtonPressed(2)) {
        gyro.reset(); // resets gyro when the button is initially PRESSED (this happens once per press)
      }
      chargeBalance();
    }
  }
}