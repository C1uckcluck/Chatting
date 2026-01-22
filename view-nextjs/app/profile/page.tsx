'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { apiUrl } from '../lib/api';

interface MemberProfile {
    username: string;
    nickname: string;
}

interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string | null;
}

export default function ProfilePage() {
    const router = useRouter();
    const [profile, setProfile] = useState<MemberProfile | null>(null);
    const [passwordCurrent, setPasswordCurrent] = useState('');
    const [passwordNext, setPasswordNext] = useState('');
    const [passwordNextConfirm, setPasswordNextConfirm] = useState('');
    const [nicknamePassword, setNicknamePassword] = useState('');
    const [nickname, setNickname] = useState('');

    const getAuthHeaders = () => {
        const token = localStorage.getItem('chatAccessToken');
        return token ? { Authorization: `Bearer ${token}` } : {};
    };

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const response = await fetch(apiUrl('/members/me'), {
                    headers: {
                        ...getAuthHeaders(),
                    },
                });
                if (response.status === 401 || response.status === 403) {
                    router.push('/login');
                    return;
                }
                if (!response.ok) {
                    throw new Error('Failed to fetch profile');
                }
                const payload: ApiResponse<MemberProfile> | null = await response.json().catch(() => null);
                if (!payload?.success) {
                    throw new Error(payload?.message || 'Failed to fetch profile');
                }
                setProfile(payload.data);
                setNickname(payload.data.nickname);
            } catch (error) {
                console.error('Profile fetch error:', error);
            }
        };

        fetchProfile();
    }, [router]);

    const handlePasswordChange = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch(apiUrl('/members/me/password'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders(),
                },
                body: JSON.stringify({
                    currentPassword: passwordCurrent,
                    newPassword: passwordNext,
                    newPasswordConfirm: passwordNextConfirm,
                }),
            });

            const payload: ApiResponse<null> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                alert(payload?.message || '비밀번호 변경에 실패했습니다.');
                return;
            }
            setPasswordCurrent('');
            setPasswordNext('');
            setPasswordNextConfirm('');
            alert(payload?.message || '비밀번호가 변경되었습니다.');
        } catch (error) {
            console.error('Password change error:', error);
            alert('비밀번호 변경 중 오류가 발생했습니다.');
        }
    };

    const handleNicknameChange = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const previousNickname = profile?.nickname;
            const response = await fetch(apiUrl('/members/me/nickname'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders(),
                },
                body: JSON.stringify({
                    currentPassword: nicknamePassword,
                    newNickname: nickname,
                }),
            });

            const payload: ApiResponse<null> | null = await response.json().catch(() => null);
            if (!response.ok || !payload?.success) {
                alert(payload?.message || '닉네임 변경에 실패했습니다.');
                return;
            }
            setNicknamePassword('');
            setProfile((prev) => (prev ? { ...prev, nickname } : prev));
            localStorage.setItem('chatNickname', nickname);
            if (previousNickname && previousNickname !== nickname) {
                const raw = localStorage.getItem('chatNicknameHistory');
                const history = raw ? (JSON.parse(raw) as string[]) : [];
                const nextHistory = history.includes(previousNickname)
                    ? history
                    : [...history, previousNickname];
                localStorage.setItem('chatNicknameHistory', JSON.stringify(nextHistory));
            }
            alert(payload?.message || '닉네임이 변경되었습니다.');
        } catch (error) {
            console.error('Nickname change error:', error);
            alert('닉네임 변경 중 오류가 발생했습니다.');
        }
    };

    return (
        <div className="container">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2>내 정보 수정</h2>
                <Link className="ghost-button" href="/">로비로 돌아가기</Link>
            </div>

            {profile && (
                <p style={{ textAlign: 'center', color: '#666' }}>
                    로그인 계정: <strong>{profile.username}</strong>
                </p>
            )}

            <div className="section">
                <h3 className="section-title">비밀번호 변경</h3>
                <form onSubmit={handlePasswordChange} className="stack">
                    <input
                        type="password"
                        placeholder="기존 비밀번호"
                        value={passwordCurrent}
                        onChange={(e) => setPasswordCurrent(e.target.value)}
                        required
                    />
                    <input
                        type="password"
                        placeholder="새 비밀번호"
                        value={passwordNext}
                        onChange={(e) => setPasswordNext(e.target.value)}
                        required
                    />
                    <input
                        type="password"
                        placeholder="새 비밀번호 확인"
                        value={passwordNextConfirm}
                        onChange={(e) => setPasswordNextConfirm(e.target.value)}
                        required
                    />
                    <button type="submit">비밀번호 변경</button>
                </form>
            </div>

            <div className="section">
                <h3 className="section-title">닉네임 변경</h3>
                <form onSubmit={handleNicknameChange} className="stack">
                    <input
                        type="password"
                        placeholder="기존 비밀번호"
                        value={nicknamePassword}
                        onChange={(e) => setNicknamePassword(e.target.value)}
                        required
                    />
                    <input
                        type="text"
                        placeholder="변경할 닉네임"
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        required
                    />
                    <button type="submit">닉네임 변경</button>
                </form>
            </div>
        </div>
    );
}
