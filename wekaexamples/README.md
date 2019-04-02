# Weka Examples

## Apache Ant

In order to compile the examples, you need to place the `weka.jar` in the
`lib` directory.

You can compile the examples as follows:

```
ant clean dist
```


## Jupyter Notebooks

[Jupyter notebooks](https://jupyter.org/) are extremely popular in the Python world,
simply because it is great to combine documentation and code in a visually appealing
way. Great tool for teaching!

Thanks to the [IJava kernel](https://github.com/SpencerPark/IJava) and the JDK 9+
*JShell* feature, it is possible to run Java within Notebooks without compiling the
code now as well.

### Installation on Linux

The following worked on Linux Mint 18.2:

* create a directory called `weka-notebooks`

  ```
  mkdir weka-notebooks
  ```

* change into the directory and create a Python virtual environment:

  ```
  cd weka-notebooks
  virtualenv -p /usr/bin/python3.5 venv
  ```

* install Jupyter notebooks and its dependencies:

  ```
  venv/bin/pip install jupyter
  ```

* then download the latest IJava [release](https://github.com/SpencerPark/IJava/releases/) (at time of writing, this was [1.20](https://github.com/SpencerPark/IJava/releases/download/v1.2.0/ijava-1.2.0.zip)) into this directory

* unzip the IJava archive:

  ```
  unzip -q ijava*.zip
  ```

* install the Java kernel into the virtual environment, using the IJava installer:

  ```
  venv/bin/python install.py --sys-prefix
  ```

* after that, fire up Jupyter using:

  ```
  venv/bin/jupyter-notebook
  ```

* now you can create new (Java) notebooks!


### Installation on Windows (using anaconda)

* open a command prompt
* create a new environment using anaconda (e.g., for Python 3.5)

  ```
  conda create -n py35-ijava python=3.5
  ```

* activate environment 

  ```
  activate py35-ijava
  ```

* install Jupyter

  ```
  pip install jupyter
  ```

* download the latest IJava [release](https://github.com/SpencerPark/IJava/releases/) (at time of writing, this was [1.20](https://github.com/SpencerPark/IJava/releases/download/v1.2.0/ijava-1.2.0.zip>))
* unzip the IJava release (e.g., with your File browser or 7-Zip)
* change into the directory where you extracted the release, containing the `install.py`, e.g.:

   ```
   cd C:\Users\fracpete\Downloads\ijava-1.2.0
   ```

* install the kernel

  ```
  python install.py --sys-prefix
  ```

* start Jupyter

  ```
  jupyter-notebook
  ```

* now you can create new (Java) notebooks!

