import RayTracingObjects as rto
import numpy as np
from PIL import Image as im
from Scene import Scene
from Camera import Camera


def main():
    title = "5_bounces_3_touching_half_specular_power"

    # set up the camera
    eye_pos = np.array([0, 0, 0]).reshape([3, ])
    eye_to = np.array([0, 0, 1]).reshape([3, ])
    cam = Camera(eye_pos, eye_to, x_res=256, y_res=256)

    p = np.array([0, -.15, 0])
    norm = np.array([0, .9, 0])
    # plane = rto.Plane(p, norm, light_source=True, light_color=np.array([0, .2, .2]), light_strength=1.0)
    plane = rto.Plane(p, norm, color=np.array([0.5, 0.5, 0.5]), specular_power=0.5)
    # plane = rto.Plane(p, norm, color=np.array([0.5, 0.5, 0.5]))
    light = rto.Plane([0, 0, -1], [0, 0, 1],
                      light_source=True, light_strength=1.0, light_color=np.array([1, 1, 1]))
    # light = rto.Sphere(1, [0, 0, -1],
    #                    light_source=True, light_color=np.array([1, 1, 1]), light_strength=1.0)

    sphere1 = rto.Sphere(.15, np.array([.15, 0, 1.5]), color=np.array([0, 1, 0]), specular_power=0.5)
    # sphere1 = rto.Sphere(1, np.array([1, 0, 3]), color=np.array([.537, .812, .941]))
    sphere2 = rto.Sphere(.15, np.array([-.15, 0, 1.5]), color=np.array([1, 0, 0]), specular_power=0.5)
    # sphere3 = rto.Sphere(5, np.array([0, 0, 7.5]), color=np.array([1, 0, 0]))

    # sphere1 = rto.Sphere(1, np.array([0, 0, 2]),
    #                      light_source=True, light_color=np.array([1, 0, 0]), light_strength=1.0)

    # scene = Scene(cam, plane, sphere1, sphere2, sphere3, color=[0, 0, 0])
    # scene = Scene(cam, plane, sphere1, color=[0, 0, 0])
    # scene = Scene(cam, plane, sphere1, color=[1, 1, 1])
    scene = Scene(cam, plane, light, sphere1, sphere2, color=[.537, .812, .941])
    # scene = Scene(cam, plane, sphere1, color=[.537, .812, .941])

    i = scene.render(n_rays=1, n_incident_rays=50, n_bounces=5)
    image = im.fromarray(i)
    image.show()
    image.save(f"ray_traced_images/{title}.png")


main()
