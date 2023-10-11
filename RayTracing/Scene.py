import numpy as np
import RayTracingObjects as rto
from Camera import Camera
from RayTraceInfo import HitInfo, MaterialInfo, RayColorInfo
from Ray import Ray, ray_in_hemisphere, specular_ray
from random import random

"""
    Holds all the objects for the scene
"""


class Scene:
    objects: list[rto.RTOType] = []
    cam: Camera = None
    ambient_color: np.ndarray = np.zeros([3, ])

    def __init__(self, camera: Camera, *obj: rto.RTOType, color=np.zeros([3, ])):
        self.color_channels = int(camera.num_channels)
        self.ambient_color = np.array(color).reshape([self.color_channels, ])
        if isinstance(camera, Camera):
            self.cam = camera
        else:
            raise TypeError("Scene initialization: Was expecting Camera, found ", type(camera))
        self.add_obj(obj)

    def __iter__(self):
        for o in self.objects:
            yield o

    def add_obj(self, obj=...):
        for o in obj:
            if isinstance(o, rto.RTOType):
                self.objects.append(o)
            else:
                raise TypeError("Scene appending: Was expecting RayTracingObject, found ", type(o))

    def render(self, n_bounces: int = 1, n_incident_rays: int = 1, n_rays: int = 1) -> np.ndarray:
        for i, j in self.cam:
            # for each pixel
            pix_color = np.zeros([self.color_channels, ])
            for r in range(n_rays):
                # get the color each ray that we are shooting out finds
                pix_color += self.get_color(self.cam.ray_through_pixel(i, j, n_bounces),
                                            n_incident_rays=n_incident_rays,
                                            n_channels=self.color_channels).ray_color
            # average them and set the color
            self.cam.set_color(i, j, pix_color/n_rays)
        return self.cam.get_image()

    def get_color(self, ray: Ray, n_incident_rays: int = 1, n_channels: int = 3) -> RayColorInfo:
        best_hit: HitInfo = HitInfo()
        for o in self:
            # try to find a hit
            current_hit: HitInfo = o.intersect(ray)
            if not current_hit.did_hit:
                continue
            # if it is better than our current hit, take note of it
            elif 0 < current_hit.t_hit < best_hit.t_hit:
                best_hit = current_hit
        # if we didn't find a valid bounce, combine the ray color with the ambient color
        if not best_hit.did_hit:
            return ray.color * RayColorInfo(n_channels, self.ambient_color)

        # pick up light from light sources, combine it with the current ray color
        color = ray.color * RayColorInfo(n_channels, best_hit.color_info)

        # if we aren't bouncing anymore, we just return the light source info
        if ray.bounces == 0:
            return color

        # if we are continuing to bounce
        # first, tint the ray with the color of the material and any emitted light
        tinted_ray = ray
        tinted_ray.color = tinted_ray.color + best_hit.color_info
        incoming_color = np.zeros([self.color_channels, ])      # holds the coloring coming in from the bounce
        spec_prob = best_hit.color_info.specular_probability    # what is the probability the bounce is specular?
        for i in range(n_incident_rays):
            # specular bounce based on the probability that a given ray on the hit object is a specular bounce
            if random() < spec_prob:
                # specular bounce
                part_color = self.get_color(specular_ray(best_hit.p_hit, best_hit.norm, tinted_ray, ray.bounces-1))
                incoming_color += part_color.ray_color
            else:
                # if not a specular bounce, do diffuse
                part_color = self.get_color(ray_in_hemisphere(best_hit.p_hit, best_hit.norm, tinted_ray, ray.bounces-1))
                incoming_color += part_color.ray_color
        # take in the colors from the incoming light
        # and add to it the color of the surface
        # (behind the scenes, multiply the color of the surface to the ray, then add in the emitted light)
        color = RayColorInfo(self.color_channels, incoming_color/n_incident_rays) + color

        # make sure we don't overflow the color
        max_c = np.max(color.ray_color)
        if max_c > 1:
            color.ray_color /= max_c

        return color
