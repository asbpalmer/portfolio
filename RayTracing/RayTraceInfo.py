import numpy as np


class RayColorInfo:
    channels: int = 3
    ray_color: np.ndarray = np.zeros([3, ])

    def __init__(self, channels: int, color):
        self.channels = int(channels)
        if isinstance(color, np.ndarray):
            self.ray_color = np.array(color).reshape([self.channels, ])
        elif isinstance(color, MaterialInfo):
            if color.emits_light:
                self.ray_color = np.array(color.emitted_strength * color.emitted_color).reshape([self.channels, ])
            else:
                self.ray_color = np.zeros([self.channels, ])

        elif isinstance(color, RayColorInfo):
            self.ray_color = color.ray_color
        else:
            raise TypeError("Could not get color info from: " + str(type(color)))
        self.ray_color = self.__scale(self.ray_color)

    def __scale(self, color: np.ndarray = ray_color):
        # scales a numpy array to be below 0
        scaled = color
        if np.max(scaled) > 1:
            scaled = scaled / np.max(scaled)
        return scaled

    def __add__(self, other):
        # add some various options together, make sure to normalize using __scale
        # handles RayColorInfo, MaterialInfo, numpy arrays, and ints
        # for material info, does ray_color * material_color + emitted_light
        if isinstance(other, RayColorInfo):
            return RayColorInfo(self.channels, self.__scale(self.ray_color + other.ray_color))
        elif isinstance(other, MaterialInfo):
            if not other.emits_light:
                emitted_light = np.zeros([self.channels, ])
            else:
                emitted_light = other.emitted_color * other.emitted_strength

            total_light = np.multiply(other.material_color, self.ray_color) + emitted_light
            return RayColorInfo(self.channels, self.__scale(total_light))
        elif isinstance(other, np.ndarray):
            combo_ray = self.ray_color + other.reshape([self.channels, ])
            return RayColorInfo(self.channels, self.__scale(combo_ray))
        elif isinstance(other, int):
            return RayColorInfo(self.channels, self.__scale(self.ray_color + other))
        else:
            raise TypeError("Cannot add RayColorInfo and " + str(type(other)))

    def __mul__(self, other):
        if isinstance(other, int):
            return RayColorInfo(self.channels, self.ray_color * other)
        elif isinstance(other, RayColorInfo):
            return RayColorInfo(self.channels, np.multiply(self.ray_color, other.ray_color))
        else:
            raise TypeError("Cannot multiply RayColorInfo and " + str(type(other)))


class MaterialInfo:
    channels: int = 3
    material_color: np.ndarray = np.zeros([3, ])
    emits_light: bool = False
    emitted_color: np.ndarray = np.zeros([3, ])
    emitted_strength: float = np.zeros([3, ])
    specular_probability: float = 0.0

    def __init__(self, channels: int, color: np.ndarray,
                 emits_light: bool = False, emitted_color: np.ndarray = np.zeros([3, ]),
                 emitted_strength: float = 0.0, specular_probability: float = 0.0):
        self.channels = int(channels)
        self.material_color = np.array(color).reshape([self.channels, ])
        self.emits_light = bool(emits_light)
        self.emitted_color = np.array(emitted_color).reshape([3, ])
        self.emitted_strength = emitted_strength
        self.specular_probability = float(specular_probability)


class HitInfo:
    did_hit: bool = False
    t_hit: float = np.PINF
    p_hit: np.ndarray = np.zeros([3, ])
    norm: np.ndarray = np.zeros([3, ])

    color_info: MaterialInfo = None

    def __init__(self, did: bool = False, t: float = np.PINF, p: np.ndarray = np.zeros([3, ]),
                 norm: np.ndarray = np.zeros([3, ]), color_info: MaterialInfo = None):
        self.did_hit = bool(did)
        self.t_hit = float(t)
        self.p_hit = np.array(p).reshape([3, ])
        self.color_info = color_info
        self.norm = norm

    def __str__(self):
        if self.did_hit:
            return f"Hit @ t={self.t_hit}, p={self.p_hit}, color={self.color_info.material_color}"
        else:
            return f"No Hit"
