{ pkgs ? import <nixpkgs> {} }:

let
  lib = pkgs.lib;

  buildGradlePackage = (import (pkgs.fetchFromBitbucket {
    owner = "dermetfan";
    repo = "nixpkgs-extras";
    rev = "afa3a8446f689b89f6139939655cf952930935a4";
    sha256 = "1w4m11s52wcwxl3zq0c9f132nbx39q4a15v214wmz83g1md7wfrf";
  }) { inherit pkgs; }).buildGradlePackage;
in buildGradlePackage {
  name = "house-of-cars";

  src = ./.;

  buildInputs = with pkgs; [
    gitMinimal
  ];

  gradleOutputs.out = {
    task = "installDist";
    paths = [ "build/install/House-of-Cars" ];
  };
}
