# Copyright 2018 Xanadu Quantum Technologies Inc.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Auxillary expectation functions
===============================

**Module name:** :mod:`pennylane_sf.expectations`

.. currentmodule:: pennylane_sf.expectations

Contains auxillary functions which convert from PennyLane-style expectations,
to the corresponding state methods in Strawberry Fields.

.. autosummary::
    identity
    mean_photon
    fock_state
    homodyne
    poly_xp


Code details
~~~~~~~~~~~~
"""
import numpy as np

import strawberryfields as sf
from strawberryfields.backends.states import BaseFockState
from strawberryfields.backends.gaussianbackend.states import GaussianState

import pennylane.ops


def identity(state, wires, params):
    """Computes the expectation value of ``qml.Identity``
    observable in Strawberry Fields, corresponding to the trace.

    Args:
        state (strawberryfields.backends.states.BaseState): the quantum state
        wires (Sequence[int]): the measured mode
        params (Sequence): sequence of parameters (not used)

    Returns:
        float, float: trace and its variance
    """
    # pylint: disable=unused-argument
    if isinstance(state, GaussianState):
        # Gaussian state representation will always have trace of 1
        return 1, 0

    N = state.num_modes
    D = state.cutoff_dim

    if N == len(wires):
        # trace of the entire system
        tr = state.trace()
        return tr, tr - tr**2

    # get the reduced density matrix
    N = len(wires)
    dm = state.reduced_dm(modes=wires)

    # construct the standard 2D density matrix, and take the trace
    new_ax = np.arange(2*N).reshape([N, 2]).T.flatten()
    tr = np.trace(dm.transpose(new_ax).reshape([D**N, D**N])).real

    return tr, tr - tr**2


def mean_photon(state, wires, params):
    """Computes the expectation value of the ``qml.NumberOperator``
    observable in Strawberry Fields.

    Args:
        state (strawberryfields.backends.states.BaseState): the quantum state
        wires (Sequence[int]): the measured mode
        params (Sequence): sequence of parameters (not used)

    Returns:
        float, float: mean photon number and its variance
    """
    # pylint: disable=unused-argument
    return state.mean_photon(wires[0])


def fock_state(state, wires, params):
    """Computes the expectation value of the ``qml.FockStateProjector``
    observable in Strawberry Fields.

    Args:
        state (strawberryfields.backends.states.BaseState): the quantum state
        wires (Sequence[int]): the measured mode
        params (Sequence): sequence of parameters

    Returns:
        float, float: Fock state probability and its variance
    """
    # pylint: disable=unused-argument
    n = params[0]
    N = state.num_modes

    if N == len(wires):
        # expectation value of the entire system
        ex = state.fock_prob(n)
        return ex, ex - ex**2

    # otherwise, we must trace out remaining systems.
    if isinstance(state, BaseFockState):
        # fock state
        dm = state.reduced_dm(modes=wires)
        ex = dm[tuple([n[i//2] for i in range(len(n)*2)])].real

    elif isinstance(state, GaussianState):
        # Reduced Gaussian state
        mu, cov = state.reduced_gaussian(modes=wires)

        # calculate reduced Q
        Q = state.qmat # pylint: disable=protected-access
        ind = np.concatenate([np.array(wires), N+np.array(wires)])
        rows = ind.reshape((-1, 1))
        cols = ind.reshape((1, -1))
        Q = Q[rows, cols]

        # calculate reduced Amat
        M = Q.shape[0]//2
        assert M == len(wires)

        I = np.identity(M)
        O = np.zeros_like(I)
        X = np.block([[O, I], [I, O]])
        A = X @ (np.identity(2*M)-np.linalg.inv(Q))

        # scale so that hbar = 2
        mu /= np.sqrt(sf.hbar/2)
        cov /= sf.hbar/2

        # create reduced Gaussian state
        new_state = GaussianState((mu, cov), M, Q, A)
        ex = new_state.fock_prob(n)

    var = ex - ex**2
    return ex, var


def homodyne(phi=None):
    """Function factory that returns the ``qml.QuadOperator`` expectation
    function for Strawberry Fields.

    ``homodyne(phi)`` returns a function

    .. code-block:: python

        homodyne_expectation(state, wires, phi)

    that is used to determine the homodyne expectation value of a wire within a
    Strawberry Fields state object, measured along a particular phase-space
    angle ``phi``.

    Note that:

    * If ``phi`` is not None, the returned function will be hardcoded to return the
      homodyne expectation value at angle ``phi`` in the phase space.

    * If ``phi`` the value of ``phi`` must be set when calling the returned function.

    Args:
        phi (float): the default phase-space axis to perform the homodyne measurement on

    Returns:
        function: a function that accepts a SF state, the wire to measure,
        and phase space angle phi, and returns the quadrature expectation
        value and variance
    """
    if phi is not None:
        return lambda state, wires, params: state.quad_expectation(wires[0], phi)

    return lambda state, wires, params: state.quad_expectation(wires[0], *params)


def poly_xp(state, wires, params):
    r"""Computes the expectation value of an observable that is a second-order
    polynomial in :math:`\{\hat{x}_i, \hat{p}_i\}_i`.

    Args:
        state (strawberryfields.backends.states.BaseState): the quantum state
        wires (Sequence[int]): measured modes
        params (Sequence[array]): Q is a matrix or vector of coefficients
            using the :math:`(\I, \x_1,\p_1, \x_2,\p_2, \dots)` ordering

    Returns:
        float, float: expectation value, variance
    """
    Q = params[0]

    # HACK, we need access to the Poly instance in order to expand the matrix!
    op = pennylane.ops.PolyXP(Q, wires=wires, do_queue=False)
    Q = op.heisenberg_obs(state.num_modes)

    if Q.ndim == 1:
        d = np.r_[Q[1::2], Q[2::2]]
        return state.poly_quad_expectation(None, d, Q[0])

    # convert to the (I, x1,x2,..., p1,p2...) ordering
    M = np.vstack((Q[0:1, :], Q[1::2, :], Q[2::2, :]))
    M = np.hstack((M[:, 0:1], M[:, 1::2], M[:, 2::2]))
    d1 = M[1:, 0]
    d2 = M[0, 1:]
    return state.poly_quad_expectation(M[1:, 1:], d1+d2, M[0, 0])
