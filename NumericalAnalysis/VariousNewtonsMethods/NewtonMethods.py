import numpy as np
from math import sqrt, fabs
from random import random, seed


# returns f with certain values of a, b, c, and t plugged in
def f(a: float, b: float, c: float, t: float, speed: float):
    def f_const(x: float, y: float, z: float, d: float):
        return sqrt((a - x)**2 + (b - y)**2 + (c - z)**2) - speed * (t - d)

    return f_const


# returns the partial derivative of f wrt x for a certain value of a, b, c, and t
def fx(a: float, b: float, c: float, t: float, speed: float):
    def fx_const(x: float, y: float, z: float, d: float):
        return (x - a)/sqrt((a - x)**2 + (b - y)**2 + (c - z)**2)

    return fx_const


# returns the partial derivative of f wrt y for a certain value of a, b, c, and t
def fy(a: float, b: float, c: float, t: float, speed: float):
    def fy_const(x: float, y: float, z: float, d: float):
        return (y - b)/sqrt((a - x)**2 + (b - y)**2 + (c - z)**2)

    return fy_const


# returns the partial derivative of f wrt z for a certain value of a, b, c, and t
def fz(a: float, b: float, c: float, t: float, speed: float):
    def fz_const(x: float, y: float, z: float, d: float):
        return (z - c)/sqrt((a - x)**2 + (b - y)**2 + (c - z)**2)

    return fz_const


# returns the partial derivative of f wrt d for a certain value of a, b, c, and t
def fd(a: float, b: float, c: float, t: float, speed: float):
    def fd_const(x: float, y: float, z: float, d: float):
        return speed

    return fd_const


# does multivariable newton's method
def multivariable_newtons(start, funcs, derivs: np.array, maxits: int) -> np.array:
    # initialize our starting point
    new_point = start.copy()
    current_f = np.zeros([len(funcs), 1])
    jacobian = np.zeros([len(funcs), len(derivs)])

    # delta = how much the iteration has changed
    # its = how many interations are we on?
    delta = 1
    its = 0
    while delta > 1e-16 and its < maxits:
        # xi = xi-1
        current_point = new_point.copy()

        # calculate the jacobian and the values of f(x)
        for i in range(len(funcs)):
            for j in range(derivs.shape[1]):
                # derivs[i][j] is holds the derivative of the ith equation wrt the jth variable
                # plug our current point into that
                jacobian[i][j] = derivs[i][j](*current_point)
            # funcs[i] holds our ith function
            # plug current point into that
            current_f[i] = funcs[i](*current_point)

        # solve -J*h = f
        h = np.linalg.solve(-jacobian, current_f)

        # set our new point
        new_point = current_point + h

        # calculate the absolute distance of the norms between last iter and this iter
        delta = fabs(np.linalg.norm(new_point) - np.linalg.norm(current_point))
        its += 1

    # returns our best guess
    return new_point


# generates 2^size unique combinations of size choices of +/-
def generate_binary(size: int) -> np.array:
    final = np.zeros(size)
    current = np.zeros(size)

    # we're going to have 2^n ways to form a string of n bits
    # iterate 2^n - 1 times since we already have a row of all zeros
    for version in range(0, 2**size-1):
        # increment the last bit by one
        current[-1] += 1

        # iterate over the size while making sure we don't have any 2's
        for i in range(1, size):

            # handle overflow
            if current[size - i] == 2:
                current[size - i] = 0
                current[size - i - 1] += 1

        # add the current bit pattern to our final array
        final = np.block([[final], [current]])
    return final


# generates an n points on a sphere of radius=radius, which have to have z > min_height
def generate_points_on_sphere(radius: float, min_height: float, n: int) -> np.array:
    points = np.array([0, 0, 0])

    # generates n points (x, y, z)
    for i in range(n):
        # z is at least min_height, but random otherwise
        z = min_height + random() * (radius - min_height)

        # y is a random value between 0 and r^2 - z^2
        random_fact = 2 * random() - 1
        y = random_fact/fabs(random_fact) * sqrt((fabs(random_fact))*(radius**2 - z**2))

        # x we can solve for with sqrt(r^2 - y^2 - z^2)
        sign = random() - 0.5
        x = sign/fabs(sign) * sqrt(radius**2 - y**2 - z**2)

        # add to our list of points
        points = np.block([[points], [np.array([x, y, z])]])

    # return all but the initial (empty) value
    return points[1:]


# gauss-newton method
# takes in start: our starting guess, funcs: our list of functions,
# derivs: a matrix of functions defining our partial derivatives, maxits: how many iterations to go over at max
def gauss_newton(start, funcs, derivs: np.array, maxits: int) -> np.array:
    # set up
    new_point = start.copy()
    current_f = np.zeros([len(funcs), 1])
    jacobian = np.zeros([len(funcs), derivs.shape[1]])

    # delta = how much change since last iteration?
    delta = 1
    # its = how many iterations are we on?
    its = 0
    while delta > 1e-16 and its < maxits:
        # xi = xi-1
        current_point = new_point.copy()

        # calculate the jacobian and the current value of f(x)
        for i in range(len(funcs)):
            for j in range(derivs.shape[1]):
                # derivs[i][j] holds the derivative of the ith function wrt the jth variable
                # plug in the current x
                jacobian[i][j] = derivs[i][j](*current_point)
            # funcs[i] holds the ith function
            # plug in the current x
            current_f[i] = funcs[i](*current_point)

        # for line spacing, store pseudo inverse = (JT J)^-1 JT
        pseudo_inverse = np.linalg.inv(jacobian.T @ jacobian) @ jacobian.T
        # xi+1 = xi - pseudo_inverse f(xi)
        new_point = current_point - pseudo_inverse @ current_f

        # how much have we changed?
        delta = fabs(np.linalg.norm(new_point) - np.linalg.norm(current_point))
        its += 1

    return new_point


# returns a lower triangular matrix where matrix[i][j] = the distance between point i and point j, where i < j
def calc_distances(point_matrix: np.array):
    # define our distance as sqrt((x1 - x2)^2 + (y1 - y2)^2 + (z1 - z2)^2)
    distance_f = lambda p1, p2: sqrt(np.array([(i - j)**2 for i, j in zip(p1, p2)]).sum())
    # initialize distance matrix to zeroes
    distances = np.zeros([point_matrix.shape[0], point_matrix.shape[0]])

    # iterate over rows
    for i in range(point_matrix.shape[0]):
        # iterate from the left to the diagonal
        for j in range(i+1):
            # calculate the distances
            distances[i][j] = distance_f(point_matrix[i], point_matrix[j])

    return distances


def main():
    # seed the distance
    seed(0)

    # Part A/B
    # define c, the speed of light, in km/s
    c_speed = 299792.458

    # define our 4 points
    point_1 = np.array([5600, 7540, 20140, 0.07074])
    point_2 = np.array([18760, 2750, 18610, 0.07220])
    point_3 = np.array([17610, 14630, 13480, 0.07690])
    point_4 = np.array([19170, 610, 18390, 0.07242])

    print("PART A/B - INFO")
    # combine our points into one matrix
    points = np.array([point_1, point_2, point_3, point_4])
    print(f"The coordinates are:")
    for i in range(1, len(points)+1):
        print(f"P{i} = (A{i} = {points[i-1][0]:10.3f}, B{i} = {points[i-1][1]:10.3f}, " +
              f"C{i} = {points[i-1][2]:10.3f}, D{i} = {points[i-1][3]:7.5f})")
    print('')

    # get the distance matrix
    distance_matrix = calc_distances(points)

    print("Distance Matrix:\n", end='')
    # print the upper triangular distance matrix
    for i in range(distance_matrix.shape[0]):
        # print horizontal labels for distance matrix
        print(i+1, end='')
        for j in range(i+1):
            # only print lower triangular
            print(f"{distance_matrix[i][j]:11.3f}", end='')
        print('')

    # print vertical labels for distance matrix
    print(" ", end='')
    for i in range(1, len(distance_matrix) + 1):
        print(f"{i:11}", end='')
    print('\n')

    # plug in values of a, b, c, and d, into f, fx, fy, fz, ft
    list_of_f = [f(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in points]
    list_of_fx = [fx(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in points]
    list_of_fy = [fy(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in points]
    list_of_fz = [fz(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in points]
    list_of_ft = [fd(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in points]

    # create an array of functions where derivs[i][j] is the ith equation wrt the jth variable
    derivs = np.array([list_of_fx, list_of_fy, list_of_fz, list_of_ft]).T

    # solve our system with our initial guess = (0, 0, 6670, 0)
    soln = multivariable_newtons(np.array([[0], [0], [6670], [0]]), list_of_f, derivs, 1000)

    # output our solution
    print("PART A/B - SOLUTION")
    print(f"The solution is:\n" +
          f"x = {soln[0][0]:10.5f}\n" +
          f"y = {soln[1][0]:10.5f}\n" +
          f"z = {soln[2][0]:10.5f}\n" +
          f"d = {soln[3][0]:10.5f}\n")

    print("Results:")
    for eq in list_of_f:
        print(eq(*soln))
    print('')

    # generate error pattern
    plus_minus_4 = (2 * generate_binary(4) - 1) * 1.0e-8

    # Part C
    print("PART C - ERROR")
    # highest value of our error
    max_error = -1
    # where the greatest error is
    greatest_error = []
    # print("       t 1234")
    for r in plus_minus_4:
        # redo part B for each combination of ti +/- 1e-8
        new_points = points.copy()
        new_points[:, 3] += r
        c_list_of_f = [f(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        c_list_of_fx = [fx(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        c_list_of_fy = [fy(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        c_list_of_fz = [fz(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        c_list_of_ft = [fd(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        c_derivs = np.array([c_list_of_fx, c_list_of_fy, c_list_of_fz, c_list_of_ft]).T
        new_soln = multivariable_newtons(np.array([[0], [0], [6670], [0]]), c_list_of_f, c_derivs, 1000)

        # find the error
        error = np.linalg.norm(new_soln - soln, ord=2)
        # if new highest error, update
        if error > max_error:
            greatest_error = r * 1e8
            max_error = error

        # print(f"Error at ", end='')
        # for i in r:
        #     if i * 1e8 == -1:
        #         print('-', end='')
        #     else:
        #         print('+', end='')
        # print(f': {error}')

    # output greatest error and +/- string
    print(f"Greatest error is: {max_error}\nOccurs at: ", end='')
    for i in greatest_error:
        if i == -1:
            print('-', end='')
        else:
            print('+', end='')
    print('\n')

    # Part D + E
    print("PART D/E - INFO")
    # create a bunch of (x, y, z) points on a sphere
    points = generate_points_on_sphere(26570, 6670, 8)

    # create a function which gets the time from given formula
    get_time = lambda point: sqrt(point[0]**2 + point[1]**2 + (point[2] - 6670)**2)/c_speed

    # add the time to our coordinates
    full_coords = np.block([points, np.array([np.apply_along_axis(get_time, 1, points)]).T])

    # print our system
    print(f"The coordinates are:")
    for i in range(1, len(full_coords)+1):
        print(f"P{i} = (A{i} = {full_coords[i-1][0]:10.3f}, B{i} = {full_coords[i-1][1]:10.3f}, " +
              f"C{i} = {full_coords[i-1][2]:10.3f}, D{i} = {full_coords[i-1][3]:7.5f})")
    print('')

    # get the distance matrix
    distance_matrix = calc_distances(full_coords)

    print("Distance Matrix:\n", end='')
    # print the upper triangular distance matrix
    for i in range(distance_matrix.shape[0]):
        # print horizontal labels for distance matrix
        print(i + 1, end='')
        for j in range(i + 1):
            # only print lower triangular
            print(f"{distance_matrix[i][j]:11.3f}", end='')
        print('')

    # print vertical labels for distance matrix
    print(" ", end='')
    for i in range(1, len(distance_matrix) + 1):
        print(f"{i:11}", end='')
    print('\n')

    # evaluate f, fx, fy, fz, ft for each point
    list_of_f = [f(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in full_coords]
    list_of_fx = [fx(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in full_coords]
    list_of_fy = [fy(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in full_coords]
    list_of_fz = [fz(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in full_coords]
    list_of_ft = [fd(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in full_coords]

    # create an array of functions where derivs[i][j] is the ith equation wrt the jth variable
    derivs = np.array([list_of_fx, list_of_fy, list_of_fz, list_of_ft]).T

    # solve our system with our initial guess = (0, 0, 6670, 0)
    soln = gauss_newton(np.array([[0], [0], [6670], [0]]), list_of_f, derivs, 1000)

    # create all 8 unique combinations of 8 +/- values
    plus_minus_8 = (2 * generate_binary(8) - 1) * 1e-8

    print("PART D/E - ERROR")
    # parameters to find our max error
    max_error = -1
    greatest_error = []
    # print("       t 12345678")
    for r in plus_minus_8:
        # solve for all changes in our values
        new_points = full_coords.copy()
        new_points[:, 3] += r
        e_list_of_f = [f(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        e_list_of_fx = [fx(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        e_list_of_fy = [fy(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        e_list_of_fz = [fz(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        e_list_of_ft = [fd(a_i, b_i, c_i, t_i, c_speed) for a_i, b_i, c_i, t_i in new_points]
        e_derivs = np.array([e_list_of_fx, e_list_of_fy, e_list_of_fz, e_list_of_ft]).T
        new_soln = gauss_newton(np.array([[0], [0], [6670], [0]]), e_list_of_f, e_derivs, 1000)

        # calculate error
        error = np.linalg.norm(new_soln - soln, ord=2)

        # print(f"Error at ", end='')
        # for i in r:
        #     if i*1e8 == -1:
        #         print('-', end='')
        #     else:
        #         print('+', end='')
        # print(f': {error}')

        # if error is greater than last error, change our greatest_error location and our max_error
        if error > max_error:
            greatest_error = r * 1e8
            max_error = error

    # output our greatest error and the bit pattern
    print(f"Greatest error is: {max_error}\nOccurs at: ", end='')
    for i in greatest_error:
        if i == -1:
            print('-', end='')
        else:
            print('+', end='')
    print('\n')

    complete_distance = distance_matrix + distance_matrix.T
    sum_distance = complete_distance.sum(axis=0)
    for dis in sum_distance:
        print(dis, end='   ')



main()
