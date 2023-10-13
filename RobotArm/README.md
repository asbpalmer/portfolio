# RobotArm
This algorithm, which I implemented for a Computer Graphics Class, uses forward kinematics, inverse kinematics, and gradient descent to navigate a robot arm through 3D space.

The robot arm tries to navigate towards different colored points while avoiding hitting brick-colored obstacles with the tip.

For this project, I created some supplemental, minimal extra algorithms (the forward_funcs library and the FunctionMatrix object) that were designed to easily allow me to calculate the position of the robot arm's tip and the Jacobian of the function that described it, and another object (RobotArm) that allowed me to manage all parts of the robot arm.

Extra dependencies:
- numpy
- vedo

To run the project with all dependencies installed, run the "InverseKinematics3d.py" folder.

What does this project demonstrate and what did I learn:
- A marked improvement in my object-oriented design from my previous projects
- Gradient descent to maneuver a complicated system through 3d space using forward and inverse kinematics
- Ahead of time handling of edge cases in the scene (objects colliding, goals being unreachable, the robot arm not having anywhere to move, etc)
- The use of python's function properties and lambda functions

If I return to this project, what will I try to implement:
- Due to time constraints with other projects, I could not implement my robot arm's complete avoidance of obstacles (only the tip avoid obstacles), so there's some clipping that I would like to do without.
- The framerate of the simulation while being run is relatively choppy, so I would like to streamline the process
- To improve speed while maintaining accuracy, I could implement various learning rate schedulers and inertia
- Extra shapes for obstacles rather than spheres

## RobotArm demonstration

Showing basic movement of the arm:

![](robot_arm_ik.gif)

Showing increased collision avoidance for the arm:

![](robot_arm_ik_collision.gif)

Showing smoother movement by changing penalty weights

![](smooth_robot_arm.gif)
