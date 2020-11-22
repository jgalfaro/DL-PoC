Supplementary Material to: Blueprint for Next Generation Cyber-
Physical Resilience using Defense Quantum Machine Learning
===

### Michel Barbeau, Carleton University, School of Computer Science, Canada.

### Joaquin Garcia-Alfaro, Institut Polytechnique de Paris, France.

## Abstract

Networked-Control Systems consist of tightly integrated computing,
communication and control technologies. Recent NCS hacking incidents
had important consequences. In such a context, reinforcing their
resilience, referring to their capacity to react, adapt and recover
from attacks, is a key challenge. Concretely, it involves the
integration of mechanisms to regulate safety, security and recovery
from adverse events, including plans deployed before, during and after
incidents occur. We envision a paradigm change where an increase of
adversarial resources does not translate anymore into higher
likelihood of disruptions. Consistently with current system design
practices in other areas, employing high safety technologies and
protocols, we outline a vision for next generation NCS with resilience
leveraging ideas as such ML and quantum computing.

*Keywords:* Resilience, Cyber-Physical Systems, Artificial Intelligence,
Machine Learning, Quantum Computing, Quantum Information, Cyber-Physical
Attacks.

*Version:* April 21, 2020

We reused code from:
<a href="https://pennylane.ai/qml/demos/tutorial_qubit_rotation.html">Basic tutorial: qubit rotation</a>

Note: This example requires python3 and version 0.8.0 of Pennylane, which is not the defaul latest version. To force installation of that version, in a Linux shell enter:

python3 -m pip install pennylane==0.8.0

or

pip3 install pennylane==0.8.0

The example also requires the StrawberryFields plugin. You can install the required plugin with the following command:

python3 -m pip install pennylane-sf

or

pip3 install pennylane-sf

## Two-train scenario

Let us consider the following discrete two-train scenario:

![figure](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/two_train.png)

Tracks are broken into sections. Let us assume a scenario where Train 1 is the victim and Train 2 is the adversary. In the Reinforcement Learning terminology, Train 1 is is the agent.
Tracks and Train 2 constitute the environment. There is an outer loop, together with a bypass from point 2 to point 6. Traversal time is uniform across sections. The normal trajectory of Train 1 is the outer loop, while maintaining a distance greated than one empty section with Train 2. When a train crosses point zero,
it has to take a decision: either traversing the outer loop or take the bypass. Both trains can take any path and take their decision independently, when they are at point zero. In this scenario, Train 1, the agent starts at point zero. Train 2 starts at point 7.

In the terms of Reinforcement Learning, both trains have two available actions: take loop and take bypass. The agent gets `k` reward points for a relative distance increase of `k` sections with Train 2. It gets `-k` reward points for a relative distance decrease of `k` sections with Train 2. For example, if both trains take the loop or if both trains decide to take the bypass, the agent gets no rewards. When Train 1 decides to take the bypass and Train 2 decides to take the loop, the agent gets two reward points, at return to point zero (Train 2 is at point five). When Train 1 decides to take the loop and Train 2 decides to take the bypass, the agent gets four reward points, at return to point zero (Train 2 is at point one).

The representation of the environment is as follows:

![track](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/MDP_two_trains.png)

In the initial state S<sub>0</sub> with a one-section separation distance,  the agent selects an action to perform: take loop or take bypass.   Train 1 performs the selected action. When selecting take loop, with probability `p` the environment goes back to state S<sub>0</sub> (no reward) or with probability `1-p` it moves to state S<sub>1</sub>,
with a five-section separation distance (reward is four). When selecting take bypass, with probability `q` the environment goes back to state S<sub>0</sub> (no reward) or with probability `1-q` it moves state S<sub>2</sub>, with a three-section separation distance (reward is two). The agent memorizes how good it has been to perform the selected action.

In the sequel, the environment probabilities of zero reward `p` and `q` are assumed to be the same.

##  PennyLan environment import


```python
import pennylane as qml
from pennylane import numpy as np
import matplotlib.pyplot as plt
```

## Device creation


```python
dev1 = qml.device("default.qubit", wires=1, shots=1)
```

## Quantum node construction

A variational circuit `W(theta)` is trained, with parameter `theta`. The circuit consists of two gates: an `X` gate and an `Y` gate. In this example, there is only state S<sub>0</sub>. It is represented by the quantum state  <img src="https://latex.codecogs.com/gif.latex?$\vert%200%20\rangle$"/> (pronounced `ket 0`). Because it is a ground state, the state is not coded explicitly at the input of the circuit. Parameter `theta` is an array of two rotation angles, one for every gate.


```python
@qml.qnode(dev1)
def W(theta):
    qml.RX(theta[0], wires=0)
    qml.RY(theta[1], wires=0)
    # return probabilities of computational basis states
    return qml.probs(wires=[0])
```

## Cost model

The actions take loop and take bypass are respectively represented by the quantum states <img src="https://latex.codecogs.com/gif.latex?$\vert%200%20\rangle$"/> and <img src="https://latex.codecogs.com/gif.latex?\vert%201%20\rangle"/> (pronounced `ket 1`). The variational circuit is trained on the probability of each computational basis state: `ket 0`  and `ket 1`.

The `cost` function measures the difference between the probablities associated to the variational circuit `W(theta)` and the target probabilities of the quantum states `ket 0`  and `ket 1`.


```python
def square_loss(X, Y):
    loss = 0
    for x, y in zip(X, Y):
        loss = loss + (x - y) ** 2
    return loss / len(X)

def cost(theta, p):
    #  p is prob. of ket zero, 1-p is prob of ket 1
    return square_loss(W(theta), [p, 1-p])
```

##  Quantum circuit update

Using optimization, the following code finds the `theta` such that the variational circuit `W(theta)` outputs a quantum state where the probabilities `ket 0`  and `ket 1` are, respectively, `p` and `1-p`.


```python
# initialise the optimizer
opt = qml.GradientDescentOptimizer(stepsize=0.4)
# set the number of steps
steps = 10
# set the initial theta value of variational circuit state
theta = np.random.randn(2)
# print("Initial probs. of basis states: {}".format(W(theta)))

def update(theta, p):
    # p = probability
    for i in range(steps):
        # update the circuit parameters
        theta = opt.step(lambda v: cost(v, p), theta)
        #if (i + 1) % 10 == 0:
            #print("Cost @ step {:5d}: {: .7f}".format(i, cost(theta,p)))
    #print("Probs. of basis states: {}".format(W(theta)))
    return theta
```

## Reinforcement learning process

Q Learning Reinforcement Learning is used.

Probabilities of quantum states `ket 0`  and `ket 1` are proportional to the respective rewards `Q[0]/(Q[0]+Q[1])` and `1 - Q[0]/(Q[0]+Q[1])`.


```python
# init theta to random values
theta = np.random.randn(2)
# number of iterations
epochs = 10
# measurement results (probs. of ket zero and ket one)
M = np.zeros(epochs)
# environment probability
p = 0.5
# rewards
R = [4, 2]
# Q-value (total reward associated with actions)
Q = [0, 0]
# learning rate
alpha = 0.5
# main loop
for i in range(epochs):
    # random action
    a = np.random.randint(2, size=1)[0]
    # determine reward
    num = np.random.random(1)[0]
    r = 0
    if num > p:
        r = R[a]
    # Bellman equation
    Q[a] = (1-alpha)*Q[a] + alpha*(r + Q[a])
    #print("action: ", a, " reward: ", r, " Q: ", Q)
    if (Q[0]+Q[1]) > 0:
        theta = update(theta, Q[0]/(Q[0]+Q[1]))
    M[i] = W(theta)[0]
```


```python
plt.plot(M, 'bs', label='take loop')
plt.plot(1-M, 'g^', label='take bypass')
plt.xlabel('Epoch')
plt.ylabel('Probability')
plt.grid(True)
plt.legend()
plt.show()
```

<kbd>[![Figure 1.](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/output_14_0.png?raw=true)]</kbd>
#### Figure 1. Evolution of DL probabilities, after some iterations, the victim will favor the take loop action, given that it gets a higher probability with respect to the take bypass action

<kbd>[![Figure 3.](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/AppletAttackMode.gif?raw=true)](https://youtu.be/SO2qjWpXH5Q)[![Figure 3.](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/AppletNominalMode.gif?raw=true)](https://youtu.be/lQLjzic_HRo)</kbd>
#### Figure 2. Graphical simulations. The left-side animation illustrates the attack mode, in which the red train is under adversarial control, seeking to perpetrate a collision with the blue train. The right-side animation illustrates the nominal mode, with the adversary under control.

<!-- <kbd>[![Figure 2.](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/attack.gif?raw=true)](https://youtu.be/__w6aChvyLs)[![Figure 2.](https://raw.githubusercontent.com/jgalfaro/DL-PoC/master/figures/attack0.gif?raw=true)](https://youtu.be/__w6aChvyLs)</kbd>
#### Figure 2. Graphical simulations. It is assumed that the red train is under adversarial control, seeking to perpetrate a collision with the blue train. Left-side animation, nominal mode. Right-side animation, attack mode.
-->
