{ pkgs ? import <nixpkgs> {} }:

let
  lib = pkgs.lib;

  buildGradlePackage = (import (pkgs.fetchFromBitbucket {
    owner = "dermetfan";
    repo = "nixpkgs-extras";
    rev = "21787267361318e205dea01d452939e88382d011";
    sha256 = "17k1m0f03lgivp48ggjcj0i5gkjjkr0mak22nh38rxr3zvhx3jxd";
  }) { inherit pkgs; }).buildGradlePackage;
in buildGradlePackage {
  name = "house-of-cars";

  src = ./.;

  gradleOutputs.out = {
    task = "installDist";
    paths = [ "build/install/House-of-Cars" ];
  };
}
