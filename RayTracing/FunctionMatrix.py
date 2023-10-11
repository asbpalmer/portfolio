#########################################################
# Function Matrix class                                 #
# written by Autumn Monsees (pmonsees2019@my.fit.edu)   #
# Used to evaluate multiple functions at a given point  #
# Also can evaluate the Jacobian                        #
# Does so analytically if derivatives are defined       #
# Approximates all other spots                          #
#########################################################

import numpy as np
from numbers import Number

class FunctionMatrix:
    _funcs = dict()
    _num_vars = 0
    _num_funcs = 0
    _ret_shape = None

    # num_vars is constant, and must be defined up front
    # everytime we add a function or derivative, we need to test to make sure it has the right amount of inputs
    # this check is based off num_vars
    # funcs we can add to and delete from later, but it represents the functions we initially plug into the matrix
    # same story with derivs, but it represents the analytical derivatives, not approximations
    def __init__(self, num_vars=0, funcs=None, derivs=None):
        self._num_vars = int(num_vars)

        # if there are functions, we account for those
        if funcs:
            # if there is just one function
            if callable(funcs):
                # add it
                self.append_func(funcs)
            else:
                # if there are multiple functions, add them one by one
                for f in range(len(funcs)):
                    self.append_func(funcs[f])

            # are there exact derivatives
            if derivs:
                # if so, are they a dictionary of dictionaries or lists
                if isinstance(derivs, dict):
                    # regardless, we need to handle them and add them to our data
                    for i, ds in derivs.items():
                        if isinstance(ds, dict):
                            for j, d in ds.items():
                                self.put_deriv(i, j, ds[j])
                        elif isinstance(ds, list):
                            for j in range(len(ds)):
                                self.put_deriv(i, j, ds[j])
                        else:
                            # throw an error if we get a bad type
                            raise TypeError("FunctionMatrix: derivs must be a list of lists/dicts"
                                            " or a dict of list/dicts")
                # if we get a list of dictionaries or lists
                elif isinstance(derivs, list):
                    for i in range(len(derivs)):
                        ds = derivs[i]
                        if isinstance(ds, dict):
                            for j, d in ds.items():
                                self.put_deriv(i, j, ds[j])
                        elif isinstance(ds, list):
                            for j in range(len(ds)):
                                self.put_deriv(i, j, ds[j])
                        else:
                            raise TypeError("FunctionMatrix: derivs must be a list of lists/dicts"
                                            " or a dict of list/dicts")
                else:
                    raise TypeError("FunctionMatrix: derivs must be a list of lists/dicts"
                                    " or a dict of list/dicts")

    # returns the new number of functions
    def append_func(self, func: callable) -> int:
        try:
            temp = func(*([0] * self._num_vars))
        except TypeError:
            raise TypeError(f"FunctionMatrix append_func: Function to be added, #{self._num_funcs+1},"
                            f" must take exactly {self._num_vars} numeric arguments.")
        except ArithmeticError:
            pass
        self._funcs[self._num_funcs] = dict({'func': func})
        self._num_funcs += 1
        return self._num_funcs

    # returns true if the value was replaced, otherwise false
    # puts func as the # func_num function, ignoring previous data
    # can also be used to append if func_num isn't specified
    def put_func(self, func: callable, func_num: int = None) -> bool:
        ret = True
        if not func_num:
            self.append_func(func)
        else:
            if not (0 < func_num <= self._num_funcs):
                raise Exception(f"FunctionMatrix replace_func: func_num (found: {func_num}) "
                                f"must be at least 0 and at most the number of functions ({self._num_funcs})")
            try:
                func(*([0] * self._num_vars))
            except TypeError:
                raise TypeError(f"FunctionMatrix replace_func: Function to be added, #{func_num}, must take exactly "
                                f"{self._num_vars} numeric arguments.")
            except ArithmeticError:
                pass
            if func_num not in self._funcs.keys():
                self._num_funcs += 1
                ret = False
            self._funcs[func_num]['func'] = func

        return ret

    # returns true if something was replaced, false otherwise
    # sets the derivative of the func_num function wrt the var_num variable as deriv
    def put_deriv(self, func_num: int, var_num: int, deriv: callable) -> bool:
        ret = False
        if func_num < 0 or func_num >= self._num_funcs:
            raise Exception(f"FunctionMatrix put_deriv: func_num (found: {func_num}) "
                            f"must be at least 0 and less than the number of functions ({self._num_funcs}).")
        if var_num < 0 or var_num >= self._num_vars:
            raise Exception(f"FunctionMatrix put_deriv: var_num (found: {var_num}) "
                            f"must be at least 0 and less than the number of variables ({self._num_vars}).")

        try:
            if 'deriv' not in self._funcs[func_num]:
                self._funcs[func_num]['deriv'] = dict()
            # check to see if we are replacing anything
            elif var_num in self._funcs[func_num]['deriv']:
                ret = True
            # we can try to add our derivative to our data and evaluate to make sure it works
            self._funcs[func_num]['deriv'][var_num] = deriv
            deriv(*([0]*self._num_vars))
        except TypeError as t:
            raise TypeError(f"FunctionMatrix put_ij_deriv: Function #{func_num}, derivative #{var_num}"
                            f" must take exactly {self._num_vars} integer arguments")
        except ValueError:
            raise ValueError(f"FunctionMatrix put_ij_deriv: deriv keys must be castable as integers.")
        except Exception:
            pass

        return ret

    # removes all the data for a function
    # returns true if the function is deleted, false otherwise
    def remove_func(self, func_num: int) -> bool:
        if func_num < 0 or func_num >= self._num_funcs:
            raise Exception(f"FunctionMatrix remove_deriv: func_num (found: {func_num}) "
                            f"must be at least 0 and less than the number of functions ({self._num_funcs})")
        ret = self._funcs.pop(func_num)
        if ret:
            self._num_funcs -= 1
            ret = True
        return ret

    # deletes all the derivs of function func_num
    # or, if deriv_num is specified, deletes the derivative of function func_num wrt the variable deriv_num
    # returns true if there is deletion, otherwise returns false
    def remove_deriv(self, func_num: int, deriv_num: int = None) -> bool:
        if not func_num or func_num < 0 or func_num >= self._num_funcs:
            raise Exception(f"FunctionMatrix remove_deriv: func_num (found: {func_num}) "
                            f"must be at least 0 and less than the number of functions ({self._num_funcs})")
        if deriv_num:
            # if there are no specified derivatives, we can't delete anything
            if 'deriv' not in self._funcs[func_num]:
                ret = False
            else:
                # since we know there is a deriv category, try to delete deriv_num
                ret = self._funcs[func_num]['deriv'].pop(deriv_num, False)
        else:
            # if we aren't trying to delete a specific derivative, we can just chop the whole thing off
            ret = self._funcs[func_num].pop('deriv', False)
        # if pop returns data, we say that we successfully deleted something
        if ret:
            ret = True
        return ret

    # evaluates our current function matrix at the given point
    def evaluate_at_point(self, *point) -> np.ndarray:
        # if we have the right amount of points, we can try to evaluate
        if len(point) != self._num_vars:
            raise Exception(f"FunctionMatrix evaluate_at_point: Was expecting {self._num_vars} point components,"
                            f" found {len(point)}.")

        # handle scalar value output
        values = np.ndarray([self._num_funcs, 1], dtype=np.float)
        for i in range(self._num_funcs):
            # evaluate each function at the given point
            v = self._funcs[i]['func'](*point)
            values[i, 0] = v

        return values

    # evaluate a jacobian at our given point with a given step size
    def jacobian_at_point(self, *point, h: float = 0.0001) -> np.ndarray:
        # if we have the right number of entries, we can evaluate
        if len(point) != self._num_vars:
            raise Exception(f"FunctionMatrix jacobian_at_point: Was expecting {self._num_vars} point components,"
                            f" found {len(point)}.")

        # initialize our jacobian
        j = np.zeros([self._num_funcs, self._num_vars], dtype=np.float)

        # for each variable, we need to modulate the value there and evaluate
        for p in range(self._num_vars):
            # copy our point and modulate
            p_mod = np.array(point, dtype=np.float)
            p_mod[p] += h

            # evaluate and store
            approx = (self.evaluate_at_point(*p_mod) - self.evaluate_at_point(*point))/h
            j[:, p] = approx[:, 0]

            # then we can replace any solutions we have with the analytical derivatives
            for f in range(self._num_funcs):
                if 'deriv' in self._funcs[f] and p in self._funcs[f]['deriv']:
                    j[f][p] = self._funcs[f]['deriv'][p](*point)

        return j

    # Creates an approximate jacobian around a certain point, which will be a bit faster
    def jacobian_approx_at_point(self, *point, h: float = 0.0001) -> np.ndarray:
        # if we have the right number of entries, we can evaluate
        if len(point) != self._num_vars:
            raise Exception(f"FunctionMatrix jacobian_at_point: Was expecting {self._num_vars} point components,"
                            f" found {len(point)}.")

        # initialize our jacobian
        j = np.zeros([self._num_funcs, self._num_vars], dtype=np.float)

        # for each variable, we need to modulate the value there and evaluate
        for p in range(self._num_vars):
            # copy our point and modulate
            p_mod = np.array(point, dtype=np.float)
            p_mod[p] += h

            # evaluate and store
            approx = (self.evaluate_at_point(*p_mod) - self.evaluate_at_point(*point))/h
            j[:, p] = approx[:, 0]

        return j

    def inv_jacobian_at_point(self, *point, h: float = 0.0001) -> np.ndarray:
        j = self.jacobian_at_point(*point, h=h)
        if self._num_vars == self._num_funcs:
            j_inv = np.linalg.inv(j)
        else:
            j_inv = np.linalg.pinv(j)
        return j_inv
