import numpy as np
from RayTraceInfo import RayColorInfo

"""
    Ray object for raytracing
"""


class Ray:
    o: np.ndarray = np.array([[0], [0], [0]])
    t: np.ndarray = np.array([[1], [0], [0]])
    d: np.ndarray = t - o
    dir: np.ndarray = d / np.linalg.norm(d)
    bounces: int = 0
    color: RayColorInfo = None

    def __init__(self, origin, towards, num_bounces=0, color_info: RayColorInfo = None):
        self.o = np.array(origin).reshape([3, ])
        self.t = np.array(towards).reshape([3, ])
        self.d = self.t - self.o
        self.dir = self.d / np.linalg.norm(self.d)
        self.bounces = num_bounces
        self.color = color_info

    def __str__(self):
        return f"Ray from {self.o} to {self.d}"

    def pos_at_t(self, t: float) -> np.ndarray:
        return self.o + self.dir * float(t)

    def set_color(self, color: RayColorInfo):
        self.color = color


def ray_in_hemisphere(p: np.ndarray, norm: np.ndarray, ray: Ray, num_bounces: int = 0) -> Ray:
    # generates a ray in a hemisphere around the norm

    new_direction = np.random.standard_normal(norm.shape)

    new_direction = new_direction * np.sign(np.dot(norm, new_direction))
    new_direction = new_direction / np.linalg.norm(new_direction) + norm

    return Ray(p, p + new_direction, num_bounces, ray.color)


def specular_ray(p: np.ndarray, norm: np.ndarray, ray: Ray, num_bounces: int = 0) -> Ray:
    # returns a ray reflected across the norm
    new_direction = ray.d - 2 * np.dot(ray.d, norm) * norm

    return Ray(p, p + new_direction, num_bounces, ray.color)
