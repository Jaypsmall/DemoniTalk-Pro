import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class DemoniAvatarWidget extends StatelessWidget {
  final double width;
  final double height;
  final int stateIndex; // 0: normal (demoni11), 1: waving (demoni22), 2: plotting (demoni33), 3: clueless (demoni44)

  const DemoniAvatarWidget({
    super.key,
    this.width = 200,
    this.height = 200,
    this.stateIndex = 0,
  });

  @override
  Widget build(BuildContext context) {
    final svgs = [
      'assets/SVGs/demoni11.svg',
      'assets/SVGs/demoni22.svg',
      'assets/SVGs/demoni33.svg',
      'assets/SVGs/demoni44.svg',
    ];
    final assetPath = svgs[stateIndex.clamp(0, svgs.length - 1)];

    return SizedBox(
      width: width,
      height: height,
      child: SvgPicture.asset(
        assetPath,
        width: width,
        height: height,
        fit: BoxFit.contain,
        placeholderBuilder: (context) => const SizedBox.shrink(),
      ),
    );
  }
}
