
package org.bonej.ops.ellipsoid;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import net.imagej.ImageJ;
import net.imagej.ops.linalg.rotate.Rotate3d;
import net.imagej.ops.special.hybrid.BinaryHybridCFI1;
import net.imagej.ops.special.hybrid.Hybrids;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scijava.vecmath.AxisAngle4d;
import org.scijava.vecmath.Matrix3d;
import org.scijava.vecmath.Matrix4d;
import org.scijava.vecmath.Vector3d;
import org.scijava.vecmath.Vector4d;

/**
 * Tests for {@link Ellipsoid}.
 *
 * @author Richard Domander
 */
public class EllipsoidTest {

	private static final ImageJ IMAGE_J = new ImageJ();
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testConstructor() throws Exception {
		// SETUP
		final double a = 1.0;
		final double b = 2.0;
		final double c = 3.0;
		final Vector3d expectedCentroid = new Vector3d(0, 0, 0);
		final Matrix4d expectedOrientation = new Matrix4d();
		expectedOrientation.setIdentity();
		final List<Vector3d> expectedAxes = Arrays.asList(new Vector3d(a, 0, 0),
			new Vector3d(0, b, 0), new Vector3d(0, 0, c));

		// EXECUTE
		final Ellipsoid ellipsoid = new Ellipsoid(b, c, a);

		// VERIFY
		assertEquals(a, ellipsoid.getA(), 1e-12);
		assertEquals(b, ellipsoid.getB(), 1e-12);
		assertEquals(c, ellipsoid.getC(), 1e-12);
		final Vector3d centroid = ellipsoid.getCentroid();
		assertNotNull("Default centroid should not be null", centroid);
		assertEquals("Default centroid should be at origin", expectedCentroid,
			centroid);
		final Matrix4d orientation = ellipsoid.getOrientation();
		assertNotNull("Default orientation matrix should not be null", orientation);
		assertEquals("Default orientation matrix should be identity",
			expectedOrientation, orientation);
		final List<Vector3d> semiAxes = ellipsoid.getSemiAxes();
		assertNotNull("Default semi-axes should not be null", semiAxes);
		for (int i = 0; i < 3; i++) {
			assertEquals("Default semi-axis is incorrect", expectedAxes.get(i),
				semiAxes.get(i));
		}
	}

	@Test
    public void testSemiAxesConstructor() throws Exception {
	    // SETUP
        final Vector3d u = new Vector3d(2, -2, 0);
        final Vector3d v = new Vector3d(1, 1, 0);
        final Vector3d w = new Vector3d(0, 0, 1);
        final List<Vector3d> normalized = Stream.of(w, v, u).map(Vector3d::new).map(e -> {
            e.normalize();
            return e;
        }).collect(toList());
        final Matrix4d expectedOrientation = new Matrix4d();
        expectedOrientation.setIdentity();
        for (int i = 0; i < 3; i++) {
            final Vector3d e = normalized.get(i);
            expectedOrientation.setColumn(i, e.x, e.y, e.z, 0);
        }

        // EXECUTE
        final Ellipsoid ellipsoid = new Ellipsoid(u, w, v);

        // VERIFY
        final List<Vector3d> semiAxes = ellipsoid.getSemiAxes();
        assertEquals(w, semiAxes.get(0));
        assertEquals(v, semiAxes.get(1));
        assertEquals(u, semiAxes.get(2));
        assertEquals(w.length(), ellipsoid.getA(), 1e-12);
        assertEquals(v.length(), ellipsoid.getB(), 1e-12);
        assertEquals(u.length(), ellipsoid.getC(), 1e-12);
        assertEquals(expectedOrientation, ellipsoid.getOrientation());
    }

    @Test(expected = NullPointerException.class)
    public void testSetSemiAxesThrowsNPEIfParameterNull() {
        final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

        ellipsoid.setSemiAxes(new Vector3d(), new Vector3d(), null);
    }

    @Test
    public void testSetSemiAxesClonesParameters() {
        final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);
        final Vector3d v = new Vector3d(0, 0, 2);
        final Vector3d original = new Vector3d(v);

        ellipsoid.setSemiAxes(new Vector3d(2, 0, 0), new Vector3d(0, 2, 0), v);

        assertFalse("Setter copied reference", v == ellipsoid.getSemiAxes().get(2));
        assertEquals("Setter changed parameter", original, v);
    }

	@Test
	public void testGetCentroid() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);
		final Vector3d centroid = new Vector3d(6, 7, 8);
		ellipsoid.setCentroid(centroid);

		final Vector3d c = ellipsoid.getCentroid();
		c.add(new Vector3d(1, 1, 1));

		assertEquals("Getter should have returned a copy, not a reference",
			centroid, ellipsoid.getCentroid());
	}

	@Test
	public void testGetOrientation() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		final Matrix4d expected = ellipsoid.getOrientation();
		final Matrix4d orientation = ellipsoid.getOrientation();

		assertFalse("Getter should have returned a copy, not a reference",
			expected == orientation);
	}

	@Test
	public void testGetSemiAxes() throws Exception {
		// SETUP
		final double a = 2.0;
		final double b = 4.0;
		final double c = 8.0;
		// @formatter:off
		final Matrix3d orientation = new Matrix3d(
				-1, 0, 0,
				0, -1, 0,
				0, 0, 1
		);
		// @formatter:on
		final List<Vector3d> expectedAxes = Arrays.asList(new Vector3d(-a, 0, 0),
			new Vector3d(0, -b, 0), new Vector3d(0, 0, c));
		final Ellipsoid ellipsoid = new Ellipsoid(b, c, a);
		ellipsoid.setOrientation(orientation);

		// EXECUTE
		final List<Vector3d> semiAxes = ellipsoid.getSemiAxes();

		// VERIFY
		for (int i = 0; i < 3; i++) {
			assertEquals("Default semi-axis is incorrect", expectedAxes.get(i),
				semiAxes.get(i));
		}
	}

	@Test
	public void testGetVolume() throws Exception {
		final double a = 2.3;
		final double b = 3.14;
		final double c = 4.25;
		final double expectedVolume = (4.0 / 3.0) * Math.PI * a * b * c;
		final Ellipsoid ellipsoid = new Ellipsoid(a, b, c);

		assertEquals(expectedVolume, ellipsoid.getVolume(), 1e-12);
	}

	@Test(expected = NullPointerException.class)
	public void testInitSamplingThrowsNPEIfOpsNull() {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.initSampling(null);
	}

	@Test
	public void testSamplePoints() throws Exception {
		// SETUP
		final double a = 2.0;
		final double b = 3.0;
		final double c = 4.0;
		final Ellipsoid ellipsoid = new Ellipsoid(a, b, c);
		final Vector3d centroid = new Vector3d(4, 5, 6);
		ellipsoid.setCentroid(centroid);
		final double sinAlpha = Math.sin(Math.PI / 4.0);
		final double cosAlpha = Math.cos(Math.PI / 4.0);
		// Orientation of an ellipsoid that's been rotated 45 deg around the z-axis
		// @formatter:off
		final Matrix3d orientation = new Matrix3d(
				cosAlpha, -sinAlpha, 0,
				sinAlpha, cosAlpha, 0,
				0, 0, 1
		);
		// @formatter:on
		final AxisAngle4d invRotation = new AxisAngle4d();
		invRotation.set(orientation);
		invRotation.setAngle(-invRotation.angle);
		final Quaterniondc q = new Quaterniond(new org.joml.AxisAngle4d(invRotation.angle, invRotation.x, invRotation.y, invRotation.z));
		final BinaryHybridCFI1<org.joml.Vector3d, Quaterniondc, org.joml.Vector3d> inverseRotation =
			Hybrids.binaryCFI1(IMAGE_J.op(), Rotate3d.class, org.joml.Vector3d.class,
					new org.joml.Vector3d(), q);
		ellipsoid.setOrientation(orientation);
		final long n = 10;
		final Function<Vector3d, Double> ellipsoidEq = (Vector3d p) -> {
			final BiFunction<Double, Double, Double> term = (x, r) -> (x * x) / (r *
				r);
			return term.apply(p.x, a) + term.apply(p.y, b) + term.apply(p.z, c);
		};
		ellipsoid.initSampling(IMAGE_J.op());

		// EXECUTE
		final List<Vector3d> points = ellipsoid.samplePoints(n);

		// VERIFY
		assertNotNull(points);
		assertEquals(n, points.size());
		// Reverse the translation and rotation so that we can assert the easier
		// equation of an axis-aligned ellipsoid at centroid
		points.forEach(p -> p.sub(centroid));
		points.stream().map(p -> new org.joml.Vector3d(p.x, p.y, p.z)).peek(
			inverseRotation::mutate).map(v -> new Vector3d(v.x, v.y, v.z)).forEach(
				p -> assertEquals("Point doesn't solve the ellipsoid equation", 1.0,
					ellipsoidEq.apply(p), 1e-5));
	}

	@Test(expected = RuntimeException.class)
	public void testSamplePointsThrowsRuntimeExceptionIfNotInitialized()
		throws Exception
	{
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.samplePoints(10);
	}

	@Test
	public void testSetA() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(6, 7, 8);

		ellipsoid.setA(5);

		assertEquals(5, ellipsoid.getA(), 1e-12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAThrowsExceptionGTB() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setA(3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAThrowsExceptionGTC() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(2, 2, 2);

		ellipsoid.setA(3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAThrowsExceptionNegativeRadius() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setA(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAThrowsExceptionNonFiniteRadius() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setA(Double.NaN);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAThrowsExceptionZeroRadius() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setA(0);
	}

	@Test
	public void testSetB() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 7, 8);

		ellipsoid.setB(4);

		assertEquals(4, ellipsoid.getB(), 1e-12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetBThrowsExceptionGTC() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setB(4);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetBThrowsExceptionLTA() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(2, 3, 4);

		ellipsoid.setB(1);
	}

	@Test
	public void testSetC() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(6, 7, 8);

		ellipsoid.setC(11);

		assertEquals(11, ellipsoid.getC(), 1e-12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetCThrowsExceptionLTA() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(2, 3, 4);

		ellipsoid.setC(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetCThrowsExceptionLTB() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(2, 3, 4);

		ellipsoid.setC(2);
	}

	@Test
	public void testSetCentroid() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);
		final Vector3d centroid = new Vector3d(6, 7, 8);

		ellipsoid.setCentroid(centroid);

		assertFalse("Setter should not copy reference", centroid == ellipsoid
			.getCentroid());
		assertEquals("Setter copied values wrong", centroid, ellipsoid
			.getCentroid());
	}

	@Test(expected = NullPointerException.class)
	public void testSetCentroidThrowsNPEIfCentroidNull() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setCentroid(null);
	}

	@Test
	public void testSetOrientation() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 4);
		// @formatter:off
		final Matrix3d orientation = new Matrix3d(
				0, -1, 0,
				1, 0, 0,
				0, 0, 1
		);
		// @formatter:on
		final Matrix3d original = new Matrix3d(orientation);

		ellipsoid.setOrientation(orientation);
		orientation.mul(1.234);

		final Matrix4d m = ellipsoid.getOrientation();
		// @formatter:off
		final Matrix3d result = new Matrix3d(
				m.m00, m.m01, m.m02,
				m.m10, m.m11, m.m12,
				m.m20, m.m21, m.m22
		);
		// @formatter:on
		assertEquals("Setter copied values incorrectly or set a reference",
			original, result);
	}

	@Test
	public void testSetOrientationAllowsLeftHandedBasis() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 4);
		// @formatter:off
		final Matrix3d leftHanded = new Matrix3d(
				1, 0, 0,
				0, 1, 0,
				0, 0, -1
		);
		// @formatter:on

		ellipsoid.setOrientation(leftHanded);
	}

	@Test
	public void testSetOrientationNormalizesVectors() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 4);
		// @formatter:off
		final Matrix3d orientation = new Matrix3d(
				3, 0, 0,
				0, 3, 0,
				0, 0, 3
		);
		// @formatter:on

		ellipsoid.setOrientation(orientation);

		final Matrix4d m = ellipsoid.getOrientation();
		for (int i = 0; i < 4; i++) {
			final Vector4d v = new Vector4d();
			m.getColumn(i, v);
			assertEquals("Vector is not a unit vector", 1.0, v.length(), 1e-12);
		}
	}

	@Test
	public void testSetOrientationThrowsIAEIfNotOrthogonalVectors()
		throws Exception
	{
		// SETUP
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);
		// @formatter:off
		final Matrix3d uVNotOrthogonal = new Matrix3d(
				1, 1, 0,
				0, 1, 0,
				0, 0, 1
		);
		final Matrix3d uWNotOrthogonal = new Matrix3d(
				1, 0, 1,
				0, 1, 0,
				0, 0, 1
		);
		final Matrix3d vWNotOrthogonal = new Matrix3d(
				1, 0, 0,
				0, 1, 1,
				0, 0, 1
		);
		// @formatter:on
		final List<Matrix3d> testMatrices = Arrays.asList(uVNotOrthogonal,
			uWNotOrthogonal, vWNotOrthogonal);
		int exceptions = 0;

		// EXECUTE
		for (final Matrix3d matrix : testMatrices) {
			try {
				ellipsoid.setOrientation(matrix);
			}
			catch (IllegalArgumentException e) {
				assertEquals("Vectors must be orthogonal", e.getMessage());
				exceptions++;
			}
		}

		// VERIFY
		assertEquals("All non-orthogonal matrices should have thrown an exception",
			testMatrices.size(), exceptions);
	}

	@Test(expected = NullPointerException.class)
	public void testSetOrientationThrowsNPEIfMatrixNull() throws Exception {
		final Ellipsoid ellipsoid = new Ellipsoid(1, 2, 3);

		ellipsoid.setOrientation(null);
	}

	@AfterClass
	public static void oneTimeTearDown() {
		IMAGE_J.context().dispose();
	}
}
