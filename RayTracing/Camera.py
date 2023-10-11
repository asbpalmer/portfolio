import numpy as np
from Ray import Ray
import forward_funcs as ff
import FunctionMatrix as FMat
from RayTraceInfo import RayColorInfo

"""
Camera class, starts at given location, looking at another given point
Has a specified focal length, which doesn't do anything yet
Has a specified resolution, both in x and y
"""


class Camera:
    loc: np.ndarray = None
    to: np.ndarray = None
    dist: float = 0.0
    f: float = 0.0
    num_channels: int = 3
    x_res: int = 256
    y_res: int = 256
    field_of_view_x: float = 0
    field_of_view_y: float = 0
    x_values: np.ndarray = None
    y_values: np.ndarray = None
    global_to_camera: np.ndarray = None
    camera_to_global: np.ndarray = None

    def __init__(self, origin, looking_at, x_res: int = 256, y_res: int = 256,
                 focal_length: float = 0.0, num_colors: int = 3, warped_lens: bool = False,
                 field_of_view_x: float = np.pi/2, field_of_view_y: float = np.pi/2, radians: bool = True):
        self.loc = np.array(origin).reshape([3, ])
        self.to = np.array(looking_at).reshape([3, ])
        self.dist = np.linalg.norm(self.to - self.loc)
        self.f = float(focal_length) if focal_length > 0 else self.dist
        self.num_channels = int(num_colors)
        self.x_res = int(x_res)
        self.y_res = int(y_res)
        self.field_of_view_x = field_of_view_x if radians else np.radians(field_of_view_x)
        self.field_of_view_y = field_of_view_y if radians else np.radians(field_of_view_y)

        if warped_lens:
            self.x_values = np.sin(np.linspace(-self.field_of_view_x/2, self.field_of_view_x/2, self.x_res))
            self.y_values = np.sin(np.linspace(self.field_of_view_y/2, -self.field_of_view_y/2, self.y_res))
        else:
            self.x_values = np.linspace(-np.sin(self.field_of_view_x / 2), np.sin(self.field_of_view_x / 2), self.x_res)
            self.y_values = np.linspace(np.sin(self.field_of_view_y / 2), -np.sin(self.field_of_view_y / 2), self.y_res)

        self.image = np.zeros([self.x_res, self.y_res, self.num_channels])

        self.__find_camera_frame()

    def __find_camera_frame(self, alpha=0.1, min_d=1e-8):
        """
        Uses Gradient Descent to find the frame for the camera with respect to the global frame.
        The inverse of this can be used to determine where a point in the camera's coordinate system is
        in the global coordinate system. The latter is generally more useful,
        as it allows us to shoot beams with reckless abandon

        :param: alpha: Accuracy and speed parameter for gradient descent
                min_d: Highest acceptable tolerance
        :return: initialized parameter in self.camera_frame and self.camera_to_global
        """
        eye_loc = self.loc
        look_to = self.to
        direction = np.array(look_to - eye_loc).reshape([3, ])
        direction = direction/np.linalg.norm(direction)

        target = np.array([0, 0, 1]).reshape([3, ])

        pos = lambda phi1, phi2, phi3: ff.rotate_3d(phi1, phi2, phi3) @ direction
        dx = lambda phi1, phi2, phi3: target - pos(phi1, phi2, phi3)

        dist = lambda phi1, phi2, phi3: np.linalg.norm(dx(phi1, phi2, phi3))

        mat = FMat.FunctionMatrix(num_vars=3, funcs=dist)
        cur_angs = np.array([0.0, 0.0, 0.0]).reshape([3, ])

        # simple gradient descent algorithm to find the angles we need to rotate the original direction by
        # that will place it at (0, 0, 1) wrt the camera
        # we can invert this to take (0, 0, 1) wrt the camera and find the geometric coordinates
        d = dist(*cur_angs)
        i = 0
        while d > min_d and i < 1000:
            j = mat.jacobian_at_point(*cur_angs)
            cur_angs = cur_angs - alpha * d * j.reshape([3, ])
            d = dist(*cur_angs)
            i += 1

        self.global_to_camera = ff.homogenous_transform(ff.rotate_3d(*cur_angs), self.loc.reshape([3, 1]))
        self.camera_to_global = np.linalg.inv(self.global_to_camera)

    def get_geo_coords(self, x_pixel: int, y_pixel: int) -> np.ndarray:
        """
        Return the geometric coordinates associated with a given pixel number
        :param x_pixel: X pixel from the left to the right
        :param y_pixel: Y pixel from the top to the bottom
        :return: The global coordinates associated with that value as a [3, ] np array
        """
        # what is the point on the focal plane that we are looking to shoot through?
        # self.f is either specified, or defaults to dist
        # x_values is a range from sin(-fov_x/2) to sin(fov_x/2)
        # same with y_values, except using fov_y
        to = self.f * np.array([self.x_values[x_pixel], self.y_values[y_pixel], 1]).reshape([3, ])

        return ff.homo_to_points(self.camera_to_global @ ff.points_to_homo(to.reshape([3, 1]))).reshape([3, ])

    def ray_through_pixel(self, x: int, y: int, num_bounces: int = 0) -> Ray:
        # shoots a ray through a given pixel, starts full white
        return Ray(self.loc, self.get_geo_coords(x, y), num_bounces,
                   RayColorInfo(self.num_channels, np.array([1, 1, 1])))

    def set_color(self, x: int, y: int, color: np.ndarray):
        # sets the color of a pixel
        c = color.reshape([self.num_channels, ])
        self.image[x, y, :] = c[:]

    def get_image(self) -> np.ndarray:
        # returns the array
        return np.uint8(self.image.transpose((1, 0, 2)) * 255)

    def __iter__(self):
        """
        Iterates over the pixels in the camera
        Row first, then column
        """
        for j in range(self.y_res):
            for i in range(self.x_res):
                yield i, j
