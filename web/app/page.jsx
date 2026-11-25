import Taller from './taller';
import { crearDemo } from '../lib/taller.mjs';

export const dynamic = 'force-dynamic';

export default function Home() {
  return <Taller inicial={crearDemo()} />;
}
