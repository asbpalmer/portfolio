#########################################################
# forward funcs library                                 #
# written by Autumn Monsees (pmonsees2019@my.fit.edu)   #
# Contains useful functions for forward kinematics      #
#########################################################

import numpy as np
from math import radians, sin, cos


def rotate_x(phi: float = 0, degree: bool = False) -> np.ndarray:
    if degree:
        phi = radians(phi)
    return np.array([
        [1,        0,         0],
        [0, cos(phi), -sin(phi)],
        [0, sin(phi),  cos(phi)]
    ])


def rotate_y(phi: float = 0, degree: bool = False) -> np.ndarray:
    if degree:
        phi = radians(phi)
    return np.array([
        [ cos(phi), 0, sin(phi)],
        [        0, 1,        0],
        [-sin(phi), 0, cos(phi)]
    ])


def rotate_z(phi: float = 0, degree: bool = False) -> np.ndarray:
    if degree:
        phi = radians(phi)
    return np.array([
        [cos(phi), -sin(phi), 0],
        [sin(phi),  cos(phi), 0],
        [       0,         0, 1]
    ])


def rotate_3d(phi_x: float = 0, phi_y: float = 0, phi_z: float = 0, degree: bool = False) -> np.ndarray:
    return rotate_x(phi_x, degree) @ rotate_y(phi_y, degree) @ rotate_z(phi_z, degree)


def homogenous_transform(transform: np.ndarray, translate: np.ndarray) -> np.ndarray:
    return np.block([
        [                   transform, translate],
        [np.zeros(transform.shape[1]),         1]
    ])


def homo_to_trans(homo: np.ndarray) -> np.ndarray:
    return homo[: -1, :-1]


def points_to_homo(points: np.ndarray) -> np.ndarray:
    return np.block([
        [points],
        [np.ones([1, points.shape[1]])]
   ])


def homo_to_points(homo: np.ndarray) -> np.ndarray:
    return homo[:-1, :]
