
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';

export default function SignupPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [nickname, setNickname] = useState('');
    const router = useRouter();

    const handleSignup = async (e: React.FormEvent) => {
        e.preventDefault();
        
        try {
            const response = await fetch('/auth/signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password, nickname }),
            });

            if (response.ok) {
                alert('회원가입 성공! 로그인해주세요.');
                router.push('/login');
            } else {
                const errorMsg = await response.text();
                alert('회원가입 실패: ' + errorMsg);
            }
        } catch (error) {
            console.error('Signup error:', error);
            alert('회원가입 중 오류가 발생했습니다.');
        }
    };

    return (
        <div className="container">
            <h2>회원가입</h2>
            <form onSubmit={handleSignup} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <div className="input-group">
                    <input
                        type="text"
                        placeholder="아이디"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                </div>
                <div className="input-group">
                    <input
                        type="password"
                        placeholder="비밀번호"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                <div className="input-group">
                    <input
                        type="text"
                        placeholder="닉네임"
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        required
                    />
                </div>
                <div className="input-group">
                    <button type="submit" style={{ width: '100%' }}>가입하기</button>
                </div>
            </form>
            <div style={{ marginTop: '20px', textAlign: 'center' }}>
                <button onClick={() => router.push('/login')} style={{ background: 'none', border: 'none', color: '#007bff', cursor: 'pointer' }}>
                    이미 계정이 있나요? 로그인
                </button>
            </div>
        </div>
    );
}
