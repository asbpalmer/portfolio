from random import random, randrange, seed
import numpy as np
import forward_funcs as ff
from FunctionMatrix import FunctionMatrix
from RobotArm import RobotArm
import vedo


def random_point_on_sphere(center=(0.0, 0.0, 0.0), radius=1.0) -> np.ndarray:
    if not isinstance(center, np.ndarray):
        center_coord = np.array([[*center]]).T
    else:
        center_coord = center

    # generate our x coord
    x = radius * (2 * random() - 1)
    # then repeatedly
    y = radius * (2 * random() - 1)
    while x**2 + y**2 > radius**2:
        y = radius * (2 * random() - 1)
    z = np.sqrt(radius**2 - x**2 - y**2)
    # flip a coin to determine if we are in the upper or lower hemisphere
    if random() < 0.5:
        z = -z

    return np.array([[x], [y], [z]]) + center_coord


# generates a random point within the sphere with center at center, radius = radius, with a possible minimum radius
# and possibly forcing that point to be above the center
def random_point_in_sphere(center=(0.0, 0.0, 0.0), radius: float = 1.0, min_r: float = 0.0, above_center: bool = False)\
        -> np.ndarray:
    if min_r > 1e-8:
        if min_r > radius:
            raise Exception(f"random_point_in_sphere: Minimum Radius ({min_r}) "
                            f"is greater than maximum radius ({radius})!")

    if not isinstance(center, np.ndarray):
        center_coord = np.array([[*center]]).T
    else:
        center_coord = center

    # generate our points, then we can refine from there
    x = radius * (2 * random() - 1)
    y = radius * (2 * random() - 1)
    z = radius * (2 * random() - 1)
    if above_center:
        z = np.abs(z)

    # x can stay constant, if we are outside the radius we generate another y
    while x ** 2 + y ** 2 > radius ** 2:
        y = radius * (2 * random() - 1)
    # then do that again for z, but we factor in the minimum radius this time
    while x ** 2 + y ** 2 + z ** 2 > radius**2 or (min_r > 1e-8 and x**2 + y**2 + z**2 < min_r**2):
        z = radius * (2 * random() - 1)
        if above_center:
            z = np.abs(z)
    point = np.array([[x], [y], [z]]) + center_coord

    return point


def robo_arm_inv_kinematics(cur_e_pos: np.ndarray, cur_angles: np.ndarray, goal_pos: np.ndarray,
                            f_mat: FunctionMatrix, rate=0.5, min_d=1e-8) -> np.ndarray:
    pos = cur_e_pos
    ang = cur_angles
    while np.abs(goal_pos - pos).sum() > min_d:
        j_inv = f_mat.inv_jacobian_at_point(*(ang.T[0]))
        ang = ang + j_inv @ (rate * (goal_pos - pos))
        pos = f_mat.evaluate_at_point(*(ang.T[0]))
    return ang


# reduces a given angle to its principle value
# 5pi/2 -> pi/2, -3pi/2 -> pi/2
def reduce_to_principle_value(phi):
    while phi > 2*np.pi:
        phi = phi - 2*np.pi
    while phi < 0:
        phi = phi + 2*np.pi
    return phi


def robo_arm_cost_function(arm: RobotArm, goal: np.ndarray, angles: np.ndarray, obstacles: list[dict] = None,
                           angle_maxes: list[float] = None, angle_mins: list[float] = None, angle_scale: float = 1,
                           angle_penalty_start: float = 0.1, obstacle_avoidance_scale: float = 1,
                           end_eff_radius: float = 1.0) -> float:
    e_pos = arm.pos_end_effector(*angles)
    goal_attraction = np.linalg.norm(goal - e_pos, ord=2)

    obstacle_avoidance = 0
    if obstacles:
        for ob in obstacles:
            place = ob['pos'].T[0]
            r = ob['radius'] + end_eff_radius
            d = np.sqrt((place[0] - e_pos[0][0])**2 +
                        (place[1] - e_pos[1][0])**2 +
                        (place[2] - e_pos[2][0])**2)
            obstacle_avoidance += np.log(r/d) if 0 < d <= r else 0

    angle_score = 0
    # reduce our angles to their principle values
    angles_shifted = np.vectorize(reduce_to_principle_value)(angles.T[0])
    if angle_maxes is not None and angle_mins is not None and angle_penalty_start > 1e-8:
        for phi, phi_max, phi_min in zip(angles_shifted, angle_maxes, angle_mins):
            if phi_max and phi_min:
                p_max = reduce_to_principle_value(phi_max)
                p_min = reduce_to_principle_value(phi_min)
                d_ang = np.abs(angle_penalty_start * (p_max - p_min))
                if p_max - d_ang < phi <= p_max:
                    angle_score += np.log(d_ang/(p_max - phi))
                elif p_min <= phi < p_min + d_ang:
                    angle_score += np.log(d_ang / (phi - p_min))

    return goal_attraction + obstacle_avoidance_scale * obstacle_avoidance + angle_scale * angle_score


def generate_obstacle(center=(0, 0, 0), max_distance: float = 1, min_distance: float = 0.25,
                      max_radius: float = 1.0, above_center: bool = False) -> (vedo.Box, np.ndarray, float):
    pos = random_point_in_sphere(center, max_distance, min_r=min_distance, above_center=above_center)
    r = max_radius * random()
    model = vedo.Sphere(r=r)
    model.texture(vedo.dataurl + 'textures/bricks.jpg')
    model.points(model.points() + pos.T)
    return model, pos, 1.5*r


def generate_goal(center=(0, 0, 0), max_distance: float = 0.75, min_distance: float = 0.25, size: float = 0.0125,
                  above_center: bool = False, colored: bool = False) -> (vedo.Sphere, np.ndarray, float):
    pos = random_point_in_sphere(center, max_distance, min_r=min_distance, above_center=above_center)
    sphere = vedo.Sphere(r=size)
    sphere.points((sphere.points().T + pos).T)
    if colored:
        sphere.c(generate_hex_color())
    return sphere, pos, size


def generate_hex_color():
    color = randrange(0, 2**24)
    return "#" + hex(color)[2:].upper()


def main():
    screenshot = False
    seed(0)
    base_height = .5
    base_radius = 0.45
    seg_1_h = 1
    seg_1_r = 0.25
    seg_2_h = 1
    seg_2_r = 0.125
    seg_3_h = 1
    seg_3_r = 0.0625
    end_eff_r = 0.1

    frame_loc = np.array([[1], [1], [0]])
    frame_ang = (0, 0, 0)

    arm = RobotArm(base_height, base_radius, seg_1_h, seg_1_r, seg_2_h, seg_2_r, seg_3_h, seg_3_r, end_eff_r,
                   frame_loc=frame_loc, frame_angs=frame_ang, base_color='red3', seg_1_color='red1',
                   seg_2_color='gray1', seg_3_color='gray3', end_eff_color='yellow', frame_c='gray2', frame_size=2)

    center = frame_loc + np.array([[0], [0], [base_height]])
    radius = np.sqrt(seg_1_h**2 + seg_2_h**2 + seg_3_h**2)
    min_r = 1.5 * base_height

    # generate goals and obstacles that are not too close to each other
    num_goals = 5
    num_obstacles = 10
    objects = []
    positions = []
    radii = []

    goal_pos =[]
    obstacles = []
    while len(goal_pos) < num_goals:
        obj, pos, rad = generate_goal(center, max_distance=0.8*radius, min_distance=min_r, size=0.025,
                                      above_center=True, colored=True)
        add = True
        for p, r in zip(positions, radii):
            if np.linalg.norm(pos - p, ord=2) < rad + r:
                add = False
                break
        if add:
            objects.append(obj)
            positions.append(pos)
            radii.append(rad)
            goal_pos.append(pos)
    while len(obstacles) < num_obstacles:
        obj, pos, rad = generate_obstacle(center, max_distance=0.8*radius, min_distance=min_r, max_radius=0.2,
                                          above_center=True)
        add = True
        for p, r in zip(positions, radii):
            if np.linalg.norm(pos - p, ord=2) < rad + r:
                add = False
                break
        if add:
            objects.append(obj)
            positions.append(pos)
            radii.append(rad)
            obstacles.append({'pos': pos, 'radius': rad})

    hx = np.array([[0.01, 0, 0]]).T
    hy = np.array([[0, 0.01, 0]]).T
    hz = np.array([[0, 0, 0.01]]).T

    init_phis = np.array([0, np.pi/3, -np.pi/2, -np.pi/4])
    arm.set_pose(*init_phis)

    ex = lambda theta1, theta2, theta3, theta4: arm.pos_end_effector(theta1, theta2, theta3, theta4)[0][0]
    ey = lambda theta1, theta2, theta3, theta4: arm.pos_end_effector(theta1, theta2, theta3, theta4)[1][0]
    ez = lambda theta1, theta2, theta3, theta4: arm.pos_end_effector(theta1, theta2, theta3, theta4)[2][0]

    e_pos = lambda theta1, theta2, theta3, theta4: arm.pos_end_effector(theta1, theta2, theta3, theta4)

    mat = FunctionMatrix(4, [ex, ey, ez])

    init_e_pos = mat.evaluate_at_point(*init_phis)

    # "body" is free to rotate
    # "shoulder" can go between 0 and 180 degrees on the horizontal
    # "elbow" can bend between 0 and 180 degrees on the vertical
    # "hand" can bend between 0 and 180 degrees on the horizontal
    phi_maxes = [None, np.pi/2, np.pi, np.pi/2]
    phi_mins = [None, -np.pi/2, 0, -np.pi/2]

    cur_e_pos = init_e_pos
    phis = np.array([init_phis]).T
    rate = 0.025

    # initialize our actors and the plotter
    arm.set_light(ambient=0.75, specular=.5)
    actors = list(arm.parts())
    for obj in objects:
        actors.append(obj)
    plotter = vedo.Plotter(axes=3, bg='#b0e0e6')
    plotter.show(actors)

    its = 0
    for pos in goal_pos:
        while np.linalg.norm(cur_e_pos - pos, ord=2) > end_eff_r/2:
            its += 1

            phix = robo_arm_inv_kinematics(cur_e_pos, phis, cur_e_pos + hx, mat)
            phiy = robo_arm_inv_kinematics(cur_e_pos, phis, cur_e_pos + hy, mat)
            phiz = robo_arm_inv_kinematics(cur_e_pos, phis, cur_e_pos + hz, mat)

            cc = robo_arm_cost_function(arm, pos, phis, obstacles=obstacles, angle_maxes=phi_maxes,
                                        angle_mins=phi_mins, angle_penalty_start=0.1, end_eff_radius=end_eff_r,
                                        obstacle_avoidance_scale=0.1, angle_scale=0.25)
            cx = robo_arm_cost_function(arm, pos, phix, obstacles=obstacles, angle_maxes=phi_maxes,
                                        angle_mins=phi_mins, angle_penalty_start=0.1, end_eff_radius=end_eff_r,
                                        obstacle_avoidance_scale=0.1, angle_scale=0.25)
            cy = robo_arm_cost_function(arm, pos, phiy, obstacles=obstacles, angle_maxes=phi_maxes,
                                        angle_mins=phi_mins, angle_penalty_start=0.1, end_eff_radius=end_eff_r,
                                        obstacle_avoidance_scale=0.1, angle_scale=0.25)
            cz = robo_arm_cost_function(arm, pos, phiz, obstacles=obstacles, angle_maxes=phi_maxes,
                                        angle_mins=phi_mins, angle_penalty_start=0.1, end_eff_radius=end_eff_r,
                                        obstacle_avoidance_scale=0.1, angle_scale=0.25)

            d = np.array([[cx], [cy], [cz]]) - cc
            new_pos = cur_e_pos - rate * d / np.linalg.norm(d, ord=2)
            phis = robo_arm_inv_kinematics(cur_e_pos, phis, new_pos, mat)
            cur_e_pos = new_pos
            arm.set_pose(*phis)
            plotter.render()

            if screenshot:
                plotter.screenshot(f"robot_images/ik_frame_{its}.png")


main()
