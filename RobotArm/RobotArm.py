import numpy as np
import vedo
import forward_funcs as ff
from math import radians


class RobotArm:
    base_height = 0
    base_radius = 0
    seg_1_length = 0
    seg_1_radius = 0
    seg_2_length = 0
    seg_2_radius = 0
    seg_3_length = 0
    seg_3_radius = 0
    e_eff_radius = 0

    # sets our base model
    base = None
    joint_b = None
    seg_1 = None
    joint_1 = None
    seg_2 = None
    joint_2 = None
    seg_3 = None
    e_eff = None

    # defines our transformed models
    _t_base = None
    _t_joint_b = None
    _t_seg_1 = None
    _t_joint_1 = None
    _t_seg_2 = None
    _t_joint_2 = None
    _t_seg_3 = None
    _t_e_eff = None

    # where is the plane our robot arm is on?
    plane = None

    # transformation matrices
    _tgf = None
    _tg0 = None
    _tg1 = None
    _tg2 = None
    _tg3 = None
    _tge = None

    # what are the current phi values
    _cur_phis = None

    def __init__(self, base_h, base_r, seg_1_l, seg_1_r, seg_2_l, seg_2_r, seg_3_l, seg_3_r, end_eff_r,
                 base_color='gray5', seg_1_color='gray5', seg_2_color='gray5', seg_3_color='gray5',
                 base_joint_color=None, joint_1_color=None, joint_2_color=None, end_eff_color=None,
                 frame_loc=np.array([[0], [0], [0]]), frame_angs=(0, 0, 0), frame_c='gray5', frame_size=1,
                 degrees: bool = False):
        if base_joint_color is None:
            base_joint_color = base_color
        if joint_1_color is None:
            joint_1_color = seg_1_color
        if joint_2_color is None:
            joint_2_color = seg_2_color
        if end_eff_color is None:
            end_eff_color = seg_3_color

        self.base_height = base_h
        self.base_radius = base_r
        self.seg_1_length = seg_1_l
        self.seg_1_radius = seg_1_r
        self.seg_2_length = seg_2_l
        self.seg_2_radius = seg_2_r
        self.seg_3_length = seg_3_l
        self.seg_3_radius = seg_3_r
        self.e_eff_radius = end_eff_r

        self.base = vedo.Cylinder(r=base_r, height=base_h, c=base_color)
        self.base.points(self.base.points() + np.array([[0, 0, base_h/2]]))

        self.joint_b = vedo.Sphere(r=base_r, c=base_joint_color)
        self.joint_b.points(self.joint_b.points() + np.array([[0, 0, base_h]]))

        self.seg_1 = vedo.Cylinder(r=seg_1_r, height=seg_1_l, c=seg_1_color)
        self.seg_1.points(self.seg_1.points() + np.array([[0, 0, seg_1_l/2]]))

        self.joint_1 = vedo.Sphere(r=seg_1_r, c=joint_1_color)
        self.joint_1.points(self.joint_1.points() + np.array([[0, 0, seg_1_l]]))

        self.seg_2 = vedo.Cylinder(r=seg_2_r, height=seg_2_l, c=seg_2_color)
        self.seg_2.points(self.seg_2.points() + np.array([[0, 0, seg_2_l/2]]))

        self.joint_2 = vedo.Sphere(r=seg_2_r, c=joint_2_color)
        self.joint_2.points(self.joint_2.points() + np.array([[0, 0, seg_2_l]]))

        self.seg_3 = vedo.Cylinder(r=seg_3_r, height=seg_3_l, c=seg_3_color)
        self.seg_3.points(self.seg_3.points() + np.array([[0, 0, seg_3_l/2]]))

        self.e_eff = vedo.Sphere(r=end_eff_r, c=end_eff_color)
        self.e_eff.points(self.e_eff.points() + np.array([[0, 0, seg_3_l]]))

        self._t_base = self.base.clone()
        self._t_joint_b = self.joint_b.clone()
        self._t_seg_1 = self.seg_1.clone()
        self._t_joint_1 = self.joint_1.clone()
        self._t_seg_2 = self.seg_2.clone()
        self._t_joint_2 = self.joint_2.clone()
        self._t_seg_3 = self.seg_3.clone()
        self._t_e_eff = self.e_eff.clone()

        self._tgf = ff.homogenous_transform(ff.rotate_3d(*frame_angs, degrees), frame_loc)
        self._tg0 = lambda phib: \
            self._tgf @ ff.homogenous_transform(ff.rotate_z(phib, degrees), np.array([[0], [0], [0]]))
        self._tg1 = lambda phib, phi1: \
            self._tg0(phib) @  ff.homogenous_transform(ff.rotate_x(phi1, degrees), np.array([[0], [0], [base_h]]))
        self._tg2 = lambda phib, phi1, phi2: \
            self._tg1(phib, phi1) @  ff.homogenous_transform(ff.rotate_x(phi2, degrees),
                                                             np.array([[0], [0], [seg_1_l]]))
        self._tg3 = lambda phib, phi1, phi2, phi3: \
            self._tg2(phib, phi1, phi2) @ ff.homogenous_transform(ff.rotate_x(phi3, degrees),
                                                                  np.array([[0], [0], [seg_2_l]]))
        self._tge = lambda phib, phi1, phi2, phi3: \
            self._tg3(phib, phi1, phi2, phi3) @ ff.points_to_homo(np.array([[0], [0], [seg_3_l]]))

        # determine the location of our base plane
        x_rot = ff.rotate_3d(*frame_angs) @ np.array([[1], [0], [0]])
        y_rot = ff.rotate_3d(*frame_angs) @ np.array([[0], [1], [0]])
        zero_rot = ff.rotate_3d(*frame_angs) @ np.array([[0], [0], [0]])

        # determine how our base plane is oriented
        zx = (zero_rot - x_rot).T
        zy = (zero_rot - y_rot).T
        norm = np.cross(zx, zy).T

        # create our table in space
        self.plane = vedo.Plane(frame_loc.T[0] - 0.001, norm, c=frame_c)
        self.plane.points((frame_size * (self.plane.points().T - frame_loc) + frame_loc).T)

        self.set_pose(0, 0, 0, 0, degrees=degrees)

    def set_pose(self, phi_b, phi_1, phi_2, phi_3, degrees: bool = False):
        if degrees:
            phi_b = radians(phi_b)
            phi_1 = radians(phi_1)
            phi_2 = radians(phi_2)
            phi_3 = radians(phi_3)

        self._cur_phis = (phi_b, phi_1, phi_2, phi_3)

        homo = ff.points_to_homo(self.base.points().T)
        new_points = ff.homo_to_points(self._tg0(phi_b) @ homo).T
        self._t_base.points(new_points)

        homo = ff.points_to_homo(self.joint_b.points().T)
        new_points = ff.homo_to_points(self._tg0(phi_b) @ homo).T
        self._t_joint_b.points(new_points)

        homo = ff.points_to_homo(self.seg_1.points().T)
        new_points = ff.homo_to_points(self._tg1(phi_b, phi_1) @ homo).T
        self._t_seg_1.points(new_points)

        homo = ff.points_to_homo(self.joint_1.points().T)
        new_points = ff.homo_to_points(self._tg1(phi_b, phi_1) @ homo).T
        self._t_joint_1.points(new_points)

        homo = ff.points_to_homo(self.seg_2.points().T)
        new_points = ff.homo_to_points(self._tg2(phi_b, phi_1, phi_2) @ homo).T
        self._t_seg_2.points(new_points)

        homo = ff.points_to_homo(self.joint_2.points().T)
        new_points = ff.homo_to_points(self._tg2(phi_b, phi_1, phi_2) @ homo).T
        self._t_joint_2.points(new_points)

        homo = ff.points_to_homo(self.seg_3.points().T)
        new_points = ff.homo_to_points(self._tg3(phi_b, phi_1, phi_2, phi_3) @ homo).T
        self._t_seg_3.points(new_points)

        homo = ff.points_to_homo(self.e_eff.points().T)
        new_points = ff.homo_to_points(self._tg3(phi_b, phi_1, phi_2, phi_3) @ homo).T
        self._t_e_eff.points(new_points)

    def parts(self):
        return self.plane, self._t_base, self._t_joint_b, self._t_seg_1, self._t_joint_1, \
               self._t_seg_2, self._t_joint_2, self._t_seg_3, self._t_e_eff

    def set_light(self, style: str = '', ambient: float = None, diffuse: float = None, specular: float = None,
                  specular_power: float = None, specular_color: float = None, metallicity: float = None,
                  roughness: float = None):
        self._t_base.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                              specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                              roughness=roughness)
        self._t_joint_b.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                                 specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                                 roughness=roughness)
        self._t_seg_1.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                               specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                               roughness=roughness)
        self._t_joint_1.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                                 specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                                 roughness=roughness)
        self._t_seg_2.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                               specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                               roughness=roughness)
        self._t_joint_2.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                                 specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                                 roughness=roughness)
        self._t_seg_3.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                               specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                               roughness=roughness)
        self._t_e_eff.lighting(style=style, ambient=ambient, diffuse=diffuse, specular=specular,
                               specular_power=specular_power, specular_color=specular_color, metallicity=metallicity,
                               roughness=roughness)

    def pos_end_effector(self, *phis, degrees: bool = False):
        if len(phis) == 0:
            ret = ff.homo_to_points(self._tge(*self._cur_phis))
        else:
            if degrees:
                ret = ff.homo_to_points(self._tge(*[radians(phi) for phi in phis]))
            else:
                ret = ff.homo_to_points(self._tge(*phis))
        return ret

    def closest_point(self, point: np.ndarray = np.array([[0], [0], [0]])) -> (float, np.ndarray):
        d = -1
        closest = np.array([[-1], [-1], [-1]])
        dom_shape = point.shape[0]
        i = 0
        while dom_shape == 1 and i < len(point.shape):
            dom_shape = point.shape[i]
            i += 1
        p = point.reshape((dom_shape, 1))

        for part in self.parts():
            if isinstance(part, vedo.pointcloud.Points):
                points = part.points().T
                print(np.sum(np.power(points - p, 2), axis=0).shape)
                # distances = (points - p)**2
                # print(distances)
        closest = p

        return d, closest

