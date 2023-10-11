import numpy as np
import scipy.sparse as sp
import scipy.sparse.linalg as splinalg
import time
from matplotlib import pyplot as pp
import pandas as pd


def u(x, y):
    return x**2 + y**2


def u_l(y):
    return y**2 + 9


def u_r(y):
    return y**2 + 25


def u_b(x):
    return x**2 + 1


def u_t(x):
    return x**2 + 4


def index(i, j, m, n):
    return i + j*m


def get_ij_from_index(ind, m, n):
    return ind % m, ind//m


def get_xy_from_ij(i, j, x_min, h_x, y_min, h_y):
    return x_min + i * h_x, y_min + j * h_y


def finite_difference(x_min, x_max, y_min, y_max, hx, hy, top, bottom, left, right, r, f):
    # find the number of divisions in x and y
    m = int((x_max - x_min)/hx + 1)
    n = int((y_max - y_min)/hy + 1)

    size = n * m

    # initialize our sparse matrix
    a = sp.lil_matrix((size, size))
    b = np.zeros((size, 1))
    for i in range(0, m):
        for j in range(0, n):

            ij = index(i, j, m, n)
            x, y = get_xy_from_ij(i, j, x_min, hx, y_min, hy)

            # left boundary
            if i == 0:
                a[ij, ij] = 1
                b[ij, 0] = left(y)

            # right boundary
            elif i == m-1:
                a[ij, ij] = 1
                b[ij, 0] = right(y)

            # bottom boundary
            elif j == 0:
                a[ij, ij] = 1
                b[ij, 0] = bottom(x)

            # top boundary
            elif j == n-1:
                a[ij, ij] = 1
                b[ij, 0] = top(x)

            # handles the interior points
            else:
                # find the indexes for the points above, below, left and right of our current point
                ij_up = index(i, j+1, m, n)
                ij_down = index(i, j-1, m, n)
                ij_left = index(i-1, j, m, n)
                ij_right = index(i+1, j, m, n)

                # calculate the coefficients of the nearby points
                a[ij, ij] = -2 * (1/hx**2 + 1/hy**2) + r(x, y)
                a[ij, ij_down] = 1/hy**2
                a[ij, ij_up] = 1/hy**2
                a[ij, ij_left] = 1/hx**2
                a[ij, ij_right] = 1/hx**2

                # get our right hand side
                b[ij, 0] = f(x, y)

    # change the matrix form and solve
    a = sp.csr_matrix(a)
    u_res = np.array([splinalg.spsolve(a, b)]).T

    return u_res


def finite_element(x_min, x_max, y_min, y_max, hx, hy, top, bottom, left, right, r, f):
    # initialize size of bounds and matrices
    m = int((x_max - x_min) / hx + 1)
    n = int((y_max - y_min) / hy + 1)

    size = n * m

    # initialize a and b
    a = sp.lil_matrix((size, size))
    b = np.zeros((size, 1))
    for i in range(0, m):
        for j in range(0, n):

            # get current index and the coordinates
            ij = index(i, j, m, n)
            x, y = get_xy_from_ij(i, j, x_min, hx, y_min, hy)

            # left boundaries
            if i == 0:
                a[ij, ij] = 1
                b[ij, 0] = left(y)

            # right boundaries
            elif i == m - 1:
                a[ij, ij] = 1
                b[ij, 0] = right(y)

            # bottom boundaries
            elif j == 0:
                a[ij, ij] = 1
                b[ij, 0] = bottom(x)

            # top boundaries
            elif j == n - 1:
                a[ij, ij] = 1
                b[ij, 0] = top(x)

            else:
                # handle interior points
                # calculate the 6 barycenters surrounding our point
                bcenters = [[x + 2 / 3 * hx, y + 1 / 3 * hy],
                            [x + 1 / 3 * hx, y + 2 / 3 * hy],
                            [x - 1 / 3 * hx, y + 1 / 3 * hy],
                            [x - 2 / 3 * hx, y - 1 / 3 * hy],
                            [x - 1 / 3 * hx, y - 2 / 3 * hy],
                            [x + 1 / 3 * hx, y - 1 / 3 * hy]]

                # calculate the value of the r function for each of the barycenters
                r_barys = np.array([r(*bar) for bar in bcenters])

                # find indices of the 6 nearby points
                ij_up = index(i, j + 1, m, n)
                ij_up_right = index(i + 1, j + 1, m, n)
                ij_down = index(i, j - 1, m, n)
                ij_down_left = index(i - 1, j - 1, m, n)
                ij_left = index(i - 1, j, m, n)
                ij_right = index(i + 1, j, m, n)

                # calculate the area of the triangle
                area_c = hx * hy / 2

                # give coefficients to the 7 points surrounding our specific point
                a[ij, ij] = 2*(hx**2 + hy**2)/(hx * hy) - r_barys.sum() * area_c/9
                a[ij, ij_down] = -hx/hy - (r_barys[4] + r_barys[5]) * area_c/9
                a[ij, ij_down_left] = -(r_barys[3] + r_barys[4]) * area_c/9
                a[ij, ij_up] = -hx/hy - (r_barys[1] + r_barys[2]) * area_c/9
                a[ij, ij_up_right] = -(r_barys[0] + r_barys[1]) * area_c/9
                a[ij, ij_left] = -hy/hx - (r_barys[2] + r_barys[3]) * area_c/9
                a[ij, ij_right] = -hy/hx - (r_barys[5] + r_barys[0]) * area_c/9

                # (sum of f(barycenter)) * (phi(barycenter) = 1/3) * (area of triangle = hx*hy/2)
                f_sum = np.array([f(*bar) for bar in bcenters]).sum()
                b[ij, 0] = -f_sum * area_c / 3
    # solve the system
    a = sp.csr_matrix(a)
    u_res = np.array([splinalg.spsolve(a, b)]).T
    # return
    return u_res


def main():
    ps = range(1, 9)
    num_trials = 30

    step_sizes = np.array([2 ** (-p) for p in ps])

    x_min = 3
    x_max = 5

    y_min = 1
    y_max = 2

    fd_data = []
    print("FINITE DIFFERENCE METHOD")
    for ex in range(len(ps)):
        trial_data = dict({})

        hx = step_sizes[ex]
        hy = step_sizes[ex]

        n = int((x_max - x_min)/hx) + 1
        m = int((y_max - y_min)/hy) + 1

        results = []

        t = time.time()
        for test in range(num_trials):
            u_calc = finite_difference(x_min, x_max, y_min, y_max, hx, hy, u_t, u_b, u_l, u_r,
                                       lambda x, y: 1/(x**2 + y**2), lambda x, y: 5).T
            results.append(u_calc)

        trial_data['p'] = ps[ex]
        trial_data['t_avg'] = (time.time() - t)/num_trials
        trial_data['h'] = hx
        trial_data['k'] = hy
        trial_data['num_trials'] = num_trials

        results = np.array(results).mean(axis=0).reshape([m, n])

        xs = np.array([x for x in np.arange(x_min, x_max + hx, hx)])
        ys = np.array([y for y in np.arange(y_min, y_max + hy, hy)])
        X, Y = np.meshgrid(xs, ys)

        exacts = X**2 + Y**2

        exacts = np.array(exacts).reshape([m, n])
        error = results - exacts
        mean_error = error.mean()
        abs_error = np.abs(error)
        mean_abs = abs_error.mean()

        trial_data['error_avg'] = mean_error
        trial_data['error_abs_mean'] = mean_abs

        fd_data.append(trial_data)

        plot_title = \
            f"h = {hx}, k = {hy}, t_avg = {trial_data['t_avg']}\n"

        data = f"TEST With p = {ex + 1}\n" + \
               f"Error from {num_trials} Trials of Finite Difference Method.\n" + \
               plot_title + \
               f"Average Error = {mean_error}\n" + \
               f"Average Absolute Error = {mean_abs}\n"
        print(data)

        fig = pp.contourf(X, Y, error)
        pp.title(plot_title)
        pp.colorbar(fig, label='Average Error')
        pp.savefig(f"finite_differences_{ex+1}_avg_error.png")
        pp.close()

        fig = pp.contourf(X, Y, abs_error)
        pp.title(plot_title)
        pp.colorbar(fig, label='Average Error Magnitude')
        pp.savefig(f"finite_differences_{ex+1}_abs_error.png")
        pp.close()

    fd_df = pd.DataFrame.from_records(fd_data).set_index('p')
    fd_df.to_csv("finite_difference.csv")

    fe_data = []
    print("\n\nFINITE ELEMENT METHOD")
    for ex in range(len(ps)):
        trial_data = dict({})

        hx = step_sizes[ex]
        hy = step_sizes[ex]

        n = int((x_max - x_min) / hx) + 1
        m = int((y_max - y_min) / hy) + 1

        results = []

        t = time.time()
        for test in range(num_trials):
            u_calc = finite_element(x_min, x_max, y_min, y_max, hx, hy, u_t, u_b, u_l, u_r,
                                    lambda x, y: 1 / (x ** 2 + y ** 2), lambda x, y: 5).T
            results.append(u_calc)

        trial_data['p'] = ps[ex]
        trial_data['t_avg'] = (time.time() - t) / num_trials
        trial_data['h'] = hx
        trial_data['k'] = hy
        trial_data['num_trials'] = num_trials

        results = np.array(results).mean(axis=0).reshape([m, n])

        xs = np.array([x for x in np.arange(x_min, x_max + hx, hx)])
        ys = np.array([y for y in np.arange(y_min, y_max + hy, hy)])
        X, Y = np.meshgrid(xs, ys)

        exacts = X ** 2 + Y ** 2

        exacts = np.array(exacts).reshape([m, n])
        error = results - exacts
        mean_error = error.mean()
        abs_error = np.abs(error)
        mean_abs = abs_error.mean()

        trial_data['error_avg'] = mean_error
        trial_data['error_abs_mean'] = mean_abs

        fe_data.append(trial_data)

        plot_title = \
            f"h = {hx}, k = {hy}, t_avg = {trial_data['t_avg']}\n"

        data = f"TEST With p = {ex + 1}\n" + \
               f"Error from {num_trials} Trials of Finite Element Method.\n" + \
               plot_title + \
               f"Average Error = {mean_error}\n" + \
               f"Average Absolute Error = {mean_abs}\n"
        print(data)

        fig = pp.contourf(X, Y, error)
        pp.title(plot_title)
        pp.colorbar(fig, label='Average Error')
        pp.savefig(f"finite_elements_{ex + 1}_avg_error.png")
        pp.close()

        fig = pp.contourf(X, Y, abs_error)
        pp.title(plot_title)
        pp.colorbar(fig, label='Average Error Magnitude')
        pp.savefig(f"finite_elements_{ex + 1}_abs_error.png")
        pp.close()

    fe_df = pd.DataFrame.from_records(fe_data).set_index('p')
    fe_df.to_csv("finite_element.csv")


main()
