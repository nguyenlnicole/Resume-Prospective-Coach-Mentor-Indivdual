export function showFeatures({ publicEdition, hostname, role, email }) {
  return (
    Boolean(publicEdition) &&
    hostname !== "adminrp.pnbpaws.com" &&
    role !== "admin"
  );
}
