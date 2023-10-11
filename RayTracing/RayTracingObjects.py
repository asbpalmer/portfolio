import numpy as np
from Ray import Ray
from RayTraceInfo import MaterialInfo, HitInfo

""" 
    Package holding various objects for raytracing (plane, sphere) derived from a super type
    A struct-like class, ColorInfo, which holds stuff like color, specular probability, etc.
    A struct-like class, HitInfo, which holds stuff like, color, and if/when/where the intersection happened
"""


class RTOType:
    color_info: MaterialInfo = None

    def get_color_info(self) -> MaterialInfo:
        return self.color_info

    def get_color(self) -> np.ndarray:
        return self.color_info.material_color

    def intersect(self, ray: Ray) -> HitInfo:
        return HitInfo()

    def get_norm(self, p) -> np.ndarray:
        return np.zeros([3, ])


class Sphere(RTOType):
    radius = 0.0
    center = np.array([[0, 0, 0]])

    def __init__(self, radius, center, color: np.ndarray = np.zeros([3, ]), channels: int = 3,
                 light_source: bool = False, light_color: np.ndarray = np.zeros([3, ]),
                 light_strength: float = 0.0, specular_power: float = 0.0):
        self.radius = radius
        self.center = np.array(center).reshape([3, ])
        self.color_info = MaterialInfo(channels, color, emits_light=light_source, emitted_color=light_color,
                                       emitted_strength=light_strength, specular_probability=specular_power)

    def intersect(self, ray: Ray) -> HitInfo:
        c_to_e = ray.o - self.center
        a = np.dot(ray.d, ray.d)
        b = 2 * np.dot(ray.d, c_to_e)
        c = np.dot(c_to_e, c_to_e) - self.radius**2

        discriminant = b**2 - 4 * a * c
        if discriminant < 0:
            return HitInfo()

        t1 = (-b + np.sqrt(discriminant))/(2 * a)
        t2 = (-b - np.sqrt(discriminant))/(2 * a)
        t = min(t1, t2)
        p = ray.pos_at_t(t)

        return HitInfo(True, t, p, self.get_norm(p), self.get_color_info())

    def get_norm(self, p) -> np.ndarray:
        point = np.array(p).reshape([3, ])
        return (point - self.center)/np.linalg.norm(point - self.center)


class Plane(RTOType):
    p: np.ndarray = None
    norm: np.ndarray = None

    def __init__(self, point1, point2, point3=None, color: np.ndarray = np.zeros([3, ]), channels: int = 3,
                 light_source: bool = False, light_color: np.ndarray = np.zeros([3, ]),
                 light_strength: float = 0.0, specular_power: float = 0.0):
        # 3 point constructor
        if not isinstance(point3, type(None)):
            p1 = np.array(point1).reshape([3, ])
            p2 = np.array(point2).reshape([3, ])
            p3 = np.array(point3).reshape([3, ])
            self.p = (p1 + p2 + p3)/3

            norm = np.cross((point3 - point1).T, (point3 - point2).T)
            self.norm = (norm/(np.linalg.norm(norm))).reshape([3, ])
        # point-norm constructor
        else:
            self.p = np.array(point1).reshape([3, ])
            norm = np.array(point2).reshape([3, ])
            self.norm = norm/np.linalg.norm(norm)

        self.color_info = MaterialInfo(channels, color, emits_light=light_source, emitted_color=light_color,
                                       emitted_strength=light_strength, specular_probability=specular_power)

    def intersect(self, ray: Ray) -> HitInfo:
        t = np.dot(self.p - ray.o, self.norm)/np.dot(ray.dir, self.norm)
        if t < 0:
            return HitInfo()
        p = ray.pos_at_t(t)
        return HitInfo(True, t, p, self.get_norm(p), self.get_color_info())

    def get_norm(self, p) -> np.ndarray:
        return self.norm

